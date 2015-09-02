package com.daehwan.learning.user;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;

public final class PolicyUtil {
	
	/**
	 * 권한을 추가할 수 있는 적절한 경로를 불러온다. 
	 * 워크플로우에서 payload는 자산이 될 수도 있고 자산 하위의 metadata가 될수도 있음.
	 * metadata가 payload 경로가 될시에 상위
	 * @param session
	 * @param path
	 * @return
	 * @throws RepositoryException
	 */
	public static String getAccessControllablePath(Session session, String path) throws RepositoryException {
		//meta or original file 경로를 asset 경로로 변환. 
		path = path.replaceAll("/jcr:content/metadata", "");
		path = path.replaceAll("/jcr:content/renditions/original", "");
		
		Node node = session.getNode(path);
		String nodeType = node.getProperty(javax.jcr.Property.JCR_PRIMARY_TYPE).getString();
		if ("dam:Asset".equals(nodeType)){
			return node.getPath();
		}
		
//		//부모들중에 asset 노드를 조회. 
//		for (Node n = node;  n.getDepth() > 1; n = n.getParent()) {
//			//노드의 타입 조
//			String type = n.getProperty(javax.jcr.Property.JCR_PRIMARY_TYPE).getString();
//			if ("dam:Asset".equals(type)){
//				return n.getPath();
//			}
//		}
		return null;
	}

	/**
	 * rep:policy 노드 존재 유무에 따른 JackrabbitAccessControlList를 가져온다. 
	 * 경로가 존재하지 않거나 잘못됐으면 null 리턴. 
	 * <code>Session.save()</code> 를 해줘야 저장됨. 
	 * 
	 * @param acm
	 * @param path
	 * @return
	 * @throws RepositoryException
	 */
	public static JackrabbitAccessControlList getJackrabbitACL(AccessControlManager acm, String path) throws RepositoryException{
		// rep:policy 노드가 있으면 가져온다. 
		AccessControlPolicy[] policies = acm.getPolicies(path);
		for (AccessControlPolicy policy : policies){
			if (policy instanceof JackrabbitAccessControlList) {
				return (JackrabbitAccessControlList) policy;
			}
		}
	    // rep:policy 노드가 없을 경우 생성. 
	    AccessControlPolicyIterator it = acm.getApplicablePolicies(path);
		while (it.hasNext()) {
		    AccessControlPolicy policy = it.nextAccessControlPolicy();
		    
		    if (policy instanceof JackrabbitAccessControlList){
		    	return (JackrabbitAccessControlList) policy;
		    }
		}
	    return null;
	}
	
	/**
	 * policy 삭제.  
	 * @param acm
	 * @param path
	 * @throws Exception
	 */
	public static void removeAllPolicy(AccessControlManager acm, String path) throws Exception{
		for (AccessControlPolicy acp : acm.getPolicies(path)){
			acm.removePolicy(path, acp);
		}
	}
	
	/**
	 * get principal. 
	 * 
	 * @param um
	 * @param name
	 * @return
	 * @throws RepositoryException
	 */
	public static Principal getPrincipal(UserManager um, String name) throws RepositoryException{
		Authorizable au = um.getAuthorizable(name);
		if (au == null) return null;
		
		return au.getPrincipal();
	}
	
	/**
	 * 
	 * 권한 부여 절차<br>
	 * <br>1. deny all - everyone (group)
	 * <br>2. allow all - administrators (group)
	 * <br>3. allow read, create, update, delete - owner (user)
	 * <br>4. allow read - [brand] (group)
	 * <br>(optional) 5. if public, read - public (group)
	 * <br>6. restore collection's permission
	 * 
	 * @param adminSession
	 * @param aclPath
	 * @return
	 * @throws RepositoryException
	 */
	public static AccessControlPolicy getDefinedAdamPolicy(Session adminSession, String aclPath) throws RepositoryException{
		JackrabbitSession session = (JackrabbitSession) adminSession;
		JackrabbitAccessControlManager acm = (JackrabbitAccessControlManager) session.getAccessControlManager();
		UserManager um = session.getUserManager();
		
		JackrabbitAccessControlList acl = getJackrabbitACL(acm, aclPath);
    	if (acl != null && !aclPath.startsWith("/content/dam/projects")) {
    		//이전 권한을 새로운 권한으로 대체한다.
        	//이전 권한 삭제
    		List<Principal> prevColGroup = new ArrayList<Principal>();
    		for (AccessControlEntry entry : acl.getAccessControlEntries()) {
    			if (entry.getPrincipal().getName().startsWith("mac-")) {
    				// collection 그룹은 유지.  
    				prevColGroup.add(entry.getPrincipal());
    			}
    			acl.removeAccessControlEntry(entry);
    		}
    		
    		// brand, owner 를 가져오기 위한 메타 정보 조회. 
    		Node meta = getAssetMetadataNode(adminSession, aclPath);
    		
    		//1. deny all - everyone (group)
    		Privilege[] allPriv = new Privilege[]{acm.privilegeFromName(Privilege.JCR_ALL)};
			acl.addEntry(PolicyUtil.getPrincipal(um, "everyone"), allPriv, false);
			
			//2. allow all - administrators (group)
			acl.addEntry(PolicyUtil.getPrincipal(um, "administrators"), allPriv, true);
			
			//3. allow read, create, update, delete - owner (user)
			String owner = meta.hasProperty("ownerId") ? 
					meta.getProperty("ownerId").getString() : session.getUserID();
			acl.addEntry(PolicyUtil.getPrincipal(um, owner), allPriv/* 전체권한부여 */, true);
			
			//4. allow read - [brand] (group)
			Privilege[] readPriv = new Privilege[]{acm.privilegeFromName(Privilege.JCR_READ)};
			String brandName = meta.hasProperty("brandCode") ? meta.getProperty("brandCode").getString() : "";
			acl.addEntry(PolicyUtil.getPrincipal(um, brandName), readPriv, true);
			
			//(optional) 5. if public, read - public (group)
			if (meta.hasProperty("isPublic") && meta.getProperty("isPublic").getBoolean()) {
    			acl.addEntry(PolicyUtil.getPrincipal(um, "public"), readPriv, true);
			}
			
			//6. Restore collection group
			for (Principal principal : prevColGroup){
				acl.addEntry(principal, readPriv, true);
			}
    	}
		return acl;
	}
	
	//<--------------------------- private methods below ----------------------------->
	
	private static Node getAssetMetadataNode(Session session, String assetPath) throws RepositoryException{
		Node node = session.getNode(assetPath);
		String nodeType = node.getProperty(javax.jcr.Property.JCR_PRIMARY_TYPE).getString();
		if ("dam:Asset".equals(nodeType)){
			return node.getNode("jcr:content/metadata");
		}
		return null;
	}
	
	/** disabled constructor */
	private PolicyUtil(){
		//do nothing
	}
}
