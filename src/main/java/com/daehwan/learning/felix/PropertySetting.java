package com.daehwan.learning.felix;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true, metatype=true, label = "Property Setting", description = "Modifiable felix setting guide")
public class PropertySetting {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Property(boolValue=true, label = "Boolean Parameter", description = "Example of a component parameter")	
	private static final String SAMPLE_BOOLEAN = "param.bool"; 

	@Property(value="default value", label = "Boolean Parameter", description = "Example of a component parameter")	
	private static final String SAMPLE_STRING = "param.string";
	
	@Property(intValue=1, label = "Boolean Parameter", description = "Example of a component parameter")	
	private static final String SAMPLE_INTEGER = "param.int";
	
	@Property(options={@PropertyOption(name="NORM",value="Norm"),
        @PropertyOption(name="MIN",value="Min"),
        @PropertyOption(name="MAX",value="Max")}
	, value="Norm", label="Select Options", description="Example of select box")
	private static final String SAMPLE_SELECT = "param.select";
	
	@Activate
	protected void activate(final Map<String, Object> props) {
		logger.debug("Property activated");
		
		for (Entry<String, Object> entry : props.entrySet()){
			logger.debug(String.format("%s : %s", entry.getKey(), entry.getValue()));
		} 
	}
	
	
}
