package com.daehwan.learning.asset;

/*
 * This Java Quick Start uses the jackrabbit-standalone-2.4.0.jar
 * file. See the previous section for the location of this JAR file
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;


public class DistributedErrorAssetProperty {

	private static final String AMORE_DEV_HOST = "http://10.155.8.84:4502/crx/server";
	private static final String AMORE_RELEASE_HOST = "http://10.129.31.164:4502/crx/server";
	private static final String AMORE_LOCAL_HOST = "http://localhost:4502/crx/server";
	
	
	public static void main(String[] args) throws Exception {
		
		final String BASE_HOST = AMORE_RELEASE_HOST;
		
		try {
			Session session = getSession(BASE_HOST, "admin", "DevJul#2015");
			if (session != null) {
				System.out.printf("%s logged in\n", BASE_HOST);
				//run(session)
				
				Node node = session.getNode("/content/dam/assets/product/product-image/v");
				
				System.out.println("Traverse Started, under " + node.getPath());
				
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
//							if (isOwnerValid) { //유효한 id 인지 확인. 
//								String ownerId = meta.getProperty("ownerId").getString();
//								Authorizable user = um.getAuthorizable(ownerId);
//								if (user != null) {
//									isValidOwner = true;
//								}
//							}
							
//							if (isBrandValid && isPublicValid && isOwnerValid && isValidOwner && isFormatValid) {
							if (isValidCopyright) {
								System.out.println(node.getPath());
								return true;
							} else {
								System.out.printf("%s\t%s\t%s\n", node.getPath(), isCopyrightPeriodFrom ? "O" : "X", isCopyrightPeriodTo ? "O" : "X");
								return false;
							}
						} catch (Exception e) {
							return false;
						}
					}
				});
				Calendar endTime = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
				System.out.printf("Start \t: %s\nEnd \t: %s\n", sdf.format(startTime.getTime()), sdf.format(endTime.getTime()));
				
				System.out.printf("Total Count : %s, Valid Asset : %s\n", totalCount, validAssetList.size());
				System.out.println("Traverse Ended.");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return
	 */
	private static Session getSession(String host, String id, String pw) {
		try {
			// Create a connection to the CQ repository running on local host
			Repository repository = JcrUtils.getRepository(host /*"http://10.155.8.84:4502/crx/server"*/);

			// Create a Session
			return repository.login(new SimpleCredentials(id, pw.toCharArray()));
			
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
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
}