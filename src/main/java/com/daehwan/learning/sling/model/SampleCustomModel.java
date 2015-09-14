package com.daehwan.learning.sling.model;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
//import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
//import org.apache.sling.models.annotations.injectorspecific.SlingObject;
 

//@Model(adaptables = { SlingHttpServletRequest.class, Resource.class }/* , defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL */)
public class SampleCustomModel {
 
  // If the field or method name doesn't exactly match the property name, @Named
  // can be used:
  @Inject
  @Named("author")
  private String authorPath;
 
  // Can use default value
  @Inject
  @Default(values = "")
  private String bodyText;
 
  // Can inject Custom Sling Object which exactly match sling object name
  // http://adobe-consulting-services.github.io/acs-aem-commons/features/aem-sling-models-injectors.html
//  @SlingObject
//  private Resource currentResource;
 
  // Can inject custom Object
  @Inject
  private ResourceResolver resourceResolver;
  
  //Inject a custom osgi service all available injector https://sling.apache.org/documentation/bundles/models.html#available-injectors
//   @Inject @Source("osgi-services") 
//   private YourService yourService;
 
  // This will return author property from current resource
  // same as properties.get("author") from jsp
  public String getAuthorPath() {
    return this.authorPath;
  }
 
  // return body text property
  public String getBodytext() {
    return this.bodyText;
  }
 
  // Example of how you can use sling Injector
  public String getResourcePath() {
    return null; ///currentResource.getPath();
  }
 
  // Example of how you can use custom injector
  public Session getSession() {
    return this.resourceResolver.adaptTo(Session.class);
  }
 
  // There are many other cool things you can do with sling model.
  // more example http://sling.apache.org/documentation/bundles/models.html	
}
