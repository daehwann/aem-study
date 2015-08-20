package com.daehwan.learning.workflow;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
//Adobe CQ Workflow APIs
import com.day.cq.workflow.model.WorkflowModel;
//Sling Imports

//This is a component so it can provide or consume services
@Component(immediate=true, metatype=true, label = "Start DAM Workflow", description = "Start DAM Workflow")
@Service(value=StartWorkflow.class)
public class StartWorkflow{
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Reference
	private WorkflowService workflowService;

	private Session session;

	@Reference
	private ResourceResolverFactory resolverFactory;
	
	/**
     * OSGi Properties *
     */
    private static final String DEFAULT_SAMPLE = "/tmp/dam/assets/product/product-image";
    private String tmpPath = DEFAULT_SAMPLE;
	@Property(label = "Workflow Model", description = "Workflow Model Path", 
  		  value = "/tmp/dam/assets/product/product-image")
	public static final String PROP_NAME = "tmp.path";
	
	@Activate
	public void activate(final Map<String, Object> config) {
		logger.info("____ACTIVATED____");
		tmpPath = PropertiesUtil.toString(config.get(PROP_NAME), DEFAULT_SAMPLE);
		
		logger.info("____RUN____" + tmpPath);
		try {
			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			session = resourceResolver.adaptTo(Session.class);
			int count = 0;
			if (session.nodeExists(tmpPath)) {
				Node tmp = session.getNode(tmpPath);
				NodeIterator itr = tmp.getNodes();
				
				while (itr.hasNext()) {
					try {
						Node target = itr.nextNode();
						String result = start(session, "/etc/workflow/models/dam/update_asset/jcr:content/model", target.getPath().replace("/tmp/", "/content/"));
						logger.info("____TRACE____ " + count);
					} catch(Exception e) {
						logger.error("{}", e.fillInStackTrace());
					}
					count++;
				}
				
			}
			
			logger.info("_____SUCCESS_____" + count);
		} catch (Exception e) {
			logger.error("{}", e.fillInStackTrace());
		}
	}
	
	public String start(Session session, String workflowName, String workflowContent) throws Exception{

		try {
			// Create a workflow session
			WorkflowSession wfSession = workflowService.getWorkflowSession(session);

			// Get the workflow model
			WorkflowModel wfModel = wfSession.getModel(workflowName);

			// Get the workflow data
			// The first param in the newWorkflowData method is the payloadType.
			// Just a fancy name to let it know what type of workflow it is
			// working with.
			WorkflowData wfData = wfSession.newWorkflowData("JCR_PATH", workflowContent);

			// Run the Workflow.
			wfSession.startWorkflow(wfModel, wfData);

			return workflowName
					+ " has been successfully invoked on this content: "
					+ workflowContent;
		} catch (Exception e) {
			throw e;
		}
	}

	
}