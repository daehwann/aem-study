package com.daehwan.learning.felix.webconsole;

import java.io.IOException;

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
	
	@Activate
	@Override
	public void activate(BundleContext ctx){
		System.out.println("________web console activated_____");
		super.activate(ctx);
		
		repositoryTracker = new ServiceTracker(ctx, SlingRepository.class.getName(), null);
		System.out.println("is tracker null?" + repositoryTracker);
        repositoryTracker.open();
	}
	
	@Override
	protected void renderContent(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		// Access required services
        final SlingRepository repository = (SlingRepository)repositoryTracker.getService();
        if(repository == null) {
            System.out.println("ERROR : NO REPOSITORY FOUND");
            return;
        }
        Session s = null;
        try {
            s = repository.loginAdministrative(repository.getDefaultWorkspace());
            //processCommands(request, pw, s, jobConsole);
            //renderJobs(req, pw, s, jobConsole);
        } catch(RepositoryException re) {
            throw new ServletException("RepositoryExceptio in renderContent()", re);
        } finally {
            if(s != null) {
                s.logout();
            }
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
