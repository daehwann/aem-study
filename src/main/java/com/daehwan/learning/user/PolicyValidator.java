package com.daehwan.learning.user;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true, metatype=true, label = "Policy Validator", description = "Policy Validator : Check Policy")
@Service(value=PolicyValidator.class)
public class PolicyValidator {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Session session;
	private UserManager um;
	private JackrabbitAccessControlManager acm;
	
	@Reference
	private ResourceResolverFactory resolverFactory;
	
	/**
     * OSGi Properties *
     */
    private static final String DEFAULT_SAMPLE = "/content/dam/assets/corporation";
    private String assetPath = DEFAULT_SAMPLE;
	@Property(label = "Traversal Asset Root", description = "Traversal Asset Root", 
  		  value = DEFAULT_SAMPLE)
	public static final String PROP_NAME = "nodetree.root";
	
	
	
	@Activate
	public void activate(final Map<String, Object> config) {
		logger.info("____ACTIVATED____");
		assetPath = PropertiesUtil.toString(config.get(PROP_NAME), DEFAULT_SAMPLE);
		
		//doProcess();
	}

	private void doProcess() {
		try {
			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			session = resourceResolver.adaptTo(Session.class);
			
			JackrabbitSession jackSession = (JackrabbitSession) session;
			acm = (JackrabbitAccessControlManager) jackSession.getAccessControlManager();
			um = jackSession.getUserManager();
			
			logger.info("___Traversal Asset Path___" + assetPath);
			
			int count = traverseTree(session.getNode(assetPath), 0);
			logger.info("___FINISHED___" + count);
		} catch (Exception e ) {
			logger.error("###ERROR___" + e.getMessage());
		}
	}
	
	private int traverseTree(Node node, int count) throws RepositoryException{
		NodeIterator itr = node.getNodes();
		while (itr.hasNext()) {
			try {
				Node n = itr.nextNode();
				String typename = n.getPrimaryNodeType().getName();
				if ("dam:Asset".equals(typename) && !isValidMetadata(n)) {
					String nodePath = n.getPath();
					AccessControlPolicy denyPolicy = getDenyPolicy(nodePath);
					session.getAccessControlManager().setPolicy(nodePath, denyPolicy);
					session.save();
					count ++;
					logger.info("___FOUND INVALID ASSET___" + count + "___" + nodePath);
				} else if ("sling:OrderedFolder".equals(typename)
						|| "sling:Folder".equals(typename)
						|| "nt:folder".equals(typename)) {
					count += traverseTree(n, count);
				}
			} catch(Exception e) {
				logger.error("###SET POLICY ERROR___" + e.getMessage());
			}
		}
		
		return count;
	}
	
	private AccessControlPolicy getDenyPolicy(String nodePath) throws RepositoryException{
		JackrabbitAccessControlList acl = PolicyUtil.getJackrabbitACL(acm, nodePath);
		if( acl!= null && acl.size() == 0) {
			//1. deny all - everyone (group)
    		Privilege[] allPriv = new Privilege[]{acm.privilegeFromName(Privilege.JCR_ALL)};
			acl.addEntry(PolicyUtil.getPrincipal(um, "everyone"), allPriv, false);
			
			//2. allow all - administrators (group)
			acl.addEntry(PolicyUtil.getPrincipal(um, "administrators"), allPriv, true);
			
		}
		return acl;
	}

	/**
	 * 
	 * @param node Asset 
	 * @return
	 */
	private boolean isValidMetadata(Node node) throws RepositoryException{
		if (node == null) return false;
		if (!node.isNodeType("dam:Asset")) return false;
		if (!node.hasNode("jcr:content/metadata")) return false;
			
		Node meta = node.getNode("jcr:content/metadata");
		boolean isBrandValid = meta.hasProperty("brandCode");
		boolean isPublicValid = meta.hasProperty("isPublic");
		boolean isOwnerValid = meta.hasProperty("ownerId");
		boolean isValidOwner = false;
		if (isOwnerValid) {
			String ownerId = meta.getProperty("ownerId").getString();
			Authorizable user = um.getAuthorizable(ownerId);
			if (user != null) {
				isValidOwner = true;
			}
		}
		
		if (isBrandValid && isPublicValid && isOwnerValid && isValidOwner) {
			return true;
		} else {
			return false;
		}
	}
}
