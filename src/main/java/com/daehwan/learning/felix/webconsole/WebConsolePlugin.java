package com.daehwan.learning.felix.webconsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.daehwan.learning.asset.RunableValidator;

/**
 * Simple webconsole which gives an overview of the topology visible by the
 * discovery service
 */
@Component(immediate=true)
@Service
@Properties({
    @Property(name=org.osgi.framework.Constants.SERVICE_DESCRIPTION,
            value="Apache Sling Web Console Plugin to make web console template"),
    @Property(name=WebConsoleConstants.PLUGIN_LABEL, value=WebConsolePlugin.LABEL),
    @Property(name=WebConsoleConstants.PLUGIN_TITLE, value=WebConsolePlugin.TITLE),
    @Property(name="felix.webconsole.configprinter.modes", value={"zip"})
})
@SuppressWarnings("serial")
public class WebConsolePlugin extends AbstractWebConsolePlugin {

	public static final String LABEL = "hello-web-console";
	public static final String TITLE = "Daehwan Web Console";
	
	private ServiceTracker repositoryTracker;
	
	private static String INPUT_TEMPLATE;
	
	@Activate
	@Override
	public void activate(BundleContext ctx){
		System.out.println("________web console activated_____");
		super.activate(ctx);
		try {
			//Get template, path : src/main/resources/base.html
			URL url = ctx.getBundle().getEntry("base.html");
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer html = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null){
				html.append(inputLine).append("\n");
			}
	        in.close();
	        INPUT_TEMPLATE = html.toString();
	        
		} catch (Exception e ) {
			e.printStackTrace();
		}
		repositoryTracker = new ServiceTracker(ctx, SlingRepository.class.getName(), null);
		if (repositoryTracker != null) {
			repositoryTracker.open();
		} else {
			System.out.println("________Web Console is not registered_____");
		}
	}
	
	@Override
	protected void renderContent(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		//set input template
		String basePath = request.getParameter("basePath");
		System.out.println("base path : " + basePath);
		INPUT_TEMPLATE = INPUT_TEMPLATE.replaceAll("<ACTION_PATH>", LABEL);
		INPUT_TEMPLATE = INPUT_TEMPLATE.replaceAll("<CURRENT_PATH>", (basePath == null ? "" : basePath));
		response.getWriter().append(INPUT_TEMPLATE);
		
		if (basePath != null && !basePath.isEmpty()) {
			try {
				boolean displayValid = request.getParameter("displayValid") != null;
				boolean displayInvalid = request.getParameter("displayInvalid") != null;
				
				System.out.println("display : " + displayValid + displayInvalid);
				
				printTreeByPath(basePath, displayValid, displayInvalid, response.getWriter());
			} catch (Exception e) {
				response.getWriter().append("ERROR " + e.getMessage());
			}
		}
	}
	
	private void printTreeByPath(String basePath, boolean displayValid, boolean displayInvalid, Writer out) 
			throws ServletException {
		// Access required services
        final SlingRepository repository = (SlingRepository)repositoryTracker.getService();
        if(repository == null) {
            System.out.println("ERROR : NO REPOSITORY FOUND");
            return;
        }
        Session session = null;
        try {
            session = repository.loginAdministrative(repository.getDefaultWorkspace());
            if (session.nodeExists(basePath)) {
            	Node node = session.getNode(basePath);
            	printTree(node, displayValid, displayInvalid, out);
            }else {
            	out.append("Path Not Found");
            }
        } catch(RepositoryException re) {
            throw new ServletException("RepositoryExceptio in renderContent()", re);
        } catch (IOException ie) {
        	throw new ServletException("IOException in renderContent()", ie);
		} finally {
            if(session != null) {
            	session.logout();
            }
        }
	}
	
	private void printTree(Node node, final boolean displayValid, final boolean displayInvalid, final Writer out) 
			throws RepositoryException, IOException {
		out.append("Traverse Started, under " + node.getPath() + "<br>");
		out.append("<table><tbody>");
		out.append("<tr class=\"content\">");
		out.append("	<th class=\"content\">Path</th>");
		out.append("	<th class=\"content\">Invalid Items (brand/public/owner/format/copyright)</th>");
		out.append("</tr>");
		
		Calendar startTime = Calendar.getInstance();
		final List<String> validAssetList = new ArrayList<String>();
		int totalCount = traverseAssetTree(node, 0, new RunableValidator() {
			public void runByNode(Node node) {
				try {
					validAssetList.add(node.getPath());
				} catch (RepositoryException e) {
					e.printStackTrace();
				}
			}
			public boolean isValidNode(Node node) {
				try {
					if (node == null) return false;
					if (!node.isNodeType("dam:Asset")) return false;
					if (!node.hasNode("jcr:content/metadata")) return false;
						
					Node meta = node.getNode("jcr:content/metadata");
					boolean isBrandValid = meta.hasProperty("brandCode");
					boolean isPublicValid = meta.hasProperty("isPublic");
					boolean isOwnerValid = meta.hasProperty("ownerId");
					boolean isFormatValid = meta.hasProperty("dc:format");

					//is validate copyright period date
					boolean isCopyrightPeriodFrom = meta.hasProperty("copyrightPeriodFrom");
					boolean isCopyrightPeriodTo = meta.hasProperty("copyrightPeriodTo");
					boolean isValidCopyright = (isCopyrightPeriodFrom && isCopyrightPeriodTo) 
											|| (!isCopyrightPeriodFrom && !isCopyrightPeriodTo);
					
					boolean isValidOwner = true; //false;
//					if (isOwnerValid) { //유효한 id 인지 확인. 
//						String ownerId = meta.getProperty("ownerId").getString();
//						Authorizable user = um.getAuthorizable(ownerId);
//						if (user != null) {
//							isValidOwner = true;
//						}
//					}
					
					if (isBrandValid && isPublicValid && isOwnerValid && isValidOwner && isFormatValid && isValidCopyright) {
//					if (isValidCopyright) {
						if (displayValid) {
							out.append("<tr class=\"content\">");
							out.append("	<td class=\"content\">" + node.getPath() + "</td>");
							out.append("	<td class=\"content\">&nbsp;</td>");
							out.append("</tr>");
						}
						return true;
					} else {
						if (displayInvalid) {
							out.append("<tr class=\"content\">");
							out.append("	<td class=\"content\">" + node.getPath() + "</td>");
							out.append("	<td class=\"content\">");
							out.append(String.format("%s/%s/%s/%s/%s", 
									(isBrandValid ? "o" : "x"), 
									(isPublicValid ? "o" : "x"), 
									(isOwnerValid ? "o" : "x"),
									(isFormatValid ? "o" : "x"),
									(isValidCopyright ? "o" : "x")));
							out.append("	</td>");
							out.append("</tr>");
							
									//, isCopyrightPeriodFrom ? "O" : "X", isCopyrightPeriodTo ? "O" : "X"));
						}
						return false;
					}
				} catch (Exception e) {
					return false;
				}
			}
		});
		Calendar endTime = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
		out.append("<tr class=\"content\">");
		out.append("	<td class=\"content\">" + String.format("Start \t: %s<br>End \t: %s", sdf.format(startTime.getTime()), sdf.format(endTime.getTime())) + "</td>");
		out.append("</tr>");
		out.append("<tr class=\"content\">");
		out.append("	<td class=\"content\">" + String.format("Total Count : %s, Valid Asset : %s<br>", totalCount, validAssetList.size()) + "</td>");
		out.append("</tr>");
		out.append("<tr class=\"content\">");
		out.append("	<td>Traverse Ended.</td>");
		out.append("</tr></tbody></table>");
	}
	
	private static int traverseAssetTree(Node node, int count, RunableValidator runable) throws RepositoryException{
		NodeIterator itr = node.getNodes();
		while (itr.hasNext()) {
			try {
				Node n = itr.nextNode();
				String typename = n.getPrimaryNodeType().getName();
				if ("dam:Asset".equals(typename) /*&& !isValidMetadata(n)*/) {
					if (runable.isValidNode(n)){
						runable.runByNode(n);
					}
					count ++;
				} else if ("sling:OrderedFolder".equals(typename)
						|| "sling:Folder".equals(typename)
						|| "nt:folder".equals(typename)) {
					count = traverseAssetTree(n, count, runable);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return count;
	}
	
	private static boolean isValidMetadata(Node node) throws RepositoryException{
		if (node == null) return false;
		if (!node.isNodeType("dam:Asset")) return false;
		if (!node.hasNode("jcr:content/metadata")) return false;
			
		Node meta = node.getNode("jcr:content/metadata");
		boolean isBrandValid = meta.hasProperty("brandCode");
		boolean isPublicValid = meta.hasProperty("isPublic");
		boolean isOwnerValid = meta.hasProperty("ownerId");
		boolean isFormatValid = meta.hasProperty("dc:format");
		
		boolean isValidOwner = true; //false;
//		if (isOwnerValid) {
//			String ownerId = meta.getProperty("ownerId").getString();
//			Authorizable user = um.getAuthorizable(ownerId);
//			if (user != null) {
//				isValidOwner = true;
//			}
//		}
		
		if (isBrandValid && isPublicValid && isOwnerValid && isValidOwner && isFormatValid) {
			return true;
		} else {
			return false;
		}
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
