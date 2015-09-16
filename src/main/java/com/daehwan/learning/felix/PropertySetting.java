package com.daehwan.learning.felix;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component(immediate=true, metatype=true, label = "Property Setting", description = "Modifiable felix setting guide")
public class PropertySetting {
	@Property(boolValue=true, label = "Boolean Parameter", description = "Example of a component parameter")	
	private static final String SAMPLE_BOOLEAN = "param.bool"; 

	@Property(value="default value", label = "Boolean Parameter", description = "Example of a component parameter")	
	private static final String SAMPLE_STRING = "param.string";
	
	@Property(intValue=1, label = "Boolean Parameter", description = "Example of a component parameter")	
	private static final String SAMPLE_INTEGER = "param.int";
	
	@Activate
	protected void activate(final Map<String, Object> props) {
		System.out.println("Property activated");
		
		for (Entry<String, Object> entry : props.entrySet()){
			System.out.printf("%s : %s\n", entry.getKey(), entry.getValue());
		}
		System.out.println(props.get(SAMPLE_BOOLEAN));
	}
}
