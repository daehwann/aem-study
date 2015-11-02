package com.daehwan.learning.felix.webconsole;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple webconsole which gives an overview of the topology visible by the
 * discovery service
 */
@Component(immediate=true)
@Service
@Properties({
    @Property(name=org.osgi.framework.Constants.SERVICE_DESCRIPTION,
            value="Group Members"),
    @Property(name=WebConsoleConstants.PLUGIN_LABEL, value=GroupMembersFinder.LABEL),
    @Property(name=WebConsoleConstants.PLUGIN_TITLE, value=GroupMembersFinder.TITLE),
    @Property(name=WebConsoleConstants.PLUGIN_CATEGORY, value="AEM Study"),
})
@SuppressWarnings("serial")
public class GroupMembersFinder extends AbstractWebConsolePlugin {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String LABEL = "groupviewer";
	public static final String TITLE = "Group Member Viewer";
	
	private final String GROUP_ROOT_PATH = "/home/groups/ap";
	private final String PARENT_ID = "profile/sobId";
	private final String MEMBER_SEPERATOR = ", ";
	
	@Reference
	private JackrabbitRepository repository;
	
	@Activate
	@Override
	public void activate(BundleContext ctx){	
		super.activate(ctx);
	}

	@Override
	protected void renderContent(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		Writer out = response.getWriter();
		
		try {
			JackrabbitSession session = (JackrabbitSession) repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
			UserManager um = session.getUserManager();
			
			Node rootNode = session.getNode(GROUP_ROOT_PATH);
			for (NodeIterator itr = rootNode.getNodes(); itr.hasNext();) {
				try {
					Node groupNode = itr.nextNode();
					String relativeGroupPath = getRelativeParentPath(rootNode, groupNode, groupNode.getName());
					
					if (!relativeGroupPath.isEmpty()) {
						//Print relative groups
						out.append(relativeGroupPath);
						
						//Print members of the group
						Authorizable au = um.getAuthorizable(groupNode.getName());
						if (au != null && au.isGroup()) {
							out.append(" : "); //GROUP : MEMBERS
							Group group = (Group) au;
							for (Iterator<Authorizable> members = group.getDeclaredMembers(); members.hasNext();) {
								Authorizable member = members.next();
								if (!member.isGroup()) { //print only members
									out.append(member.getID());
									
									if (members.hasNext()) {
										out.append(MEMBER_SEPERATOR);
									}
								}
							}
						}
						out.append("<br>");
					}
				} catch (Exception e) {
					logger.error("ERROR : " + e.getMessage());
				}
			}
		} catch (RepositoryException e) {
			logger.error("Render Content Error : " + e.getMessage());
		}
	}

	private String getRelativeParentPath(Node rootNode, Node node, String path){
		
		try {
			if (node.hasProperty(PARENT_ID)) {
				String sobId = node.getProperty(PARENT_ID).getString();
				if (rootNode.hasNode(sobId)){
					Node parentTeam = rootNode.getNode(sobId);
					return getRelativeParentPath(rootNode, parentTeam, sobId + "/" + path);
				} else {
					return sobId + "/" + path;
				}
			}
		}catch (Exception e) {
			logger.error("ERROR");
		}
		return "";
	}
	

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return LABEL;
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return TITLE;
	}
}