package com.daehwan.learning.sling.resource;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * 
 * @author daehwan
 *
 */
//@Service(value=SampleResourceWriter.class)
//@Component(/*immediate=true*/)
public class SampleResourceWriter{
 
    private final Logger logger = LoggerFactory.getLogger(getClass());
 
    @Reference
    private ResourceResolverFactory resolverFactory;
 
    @Activate
    public void doAWriteOperat£ion(ComponentContext ctx) {
    	/**
    	 * ResourceResolver는 각각의 서비스 별로 다른 권한을 가진다.
    	 * 서비스의 권한은 매핑된 유저의 권한을 가진다.
    	 * 
    	 * ‘serviceName [ ":" subServiceName ] “=” username’.
		 *
		 *	serviceName: £The symbolic name of the bundle which provides this service.
		 *	subServiceName: The value of the passed attribute with the ResourceResolverFactory.SUBSERVICE key.
		 *	username: The username of the user which should be mapped with this service.
    	 * 
    	 */
        Map<String, Object> param = new HashMap<String, Object>();
        param.put(ResourceResolverFactory.SUBSERVICE, "sampleWriteService");
        ResourceResolver resolver = null;
        try {
            resolver = resolverFactory.getServiceResourceResolver(param);
            logger.info(resolver.getUserID());
            
        } catch (LoginException e) {
            logger.error("LoginException",e);
        }finally{
            if(resolver != null && resolver.isLive()){
                resolver.close();
            }
        }
    }
    
    
}