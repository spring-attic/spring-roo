package org.springframework.roo.classpath.scanner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.model.CustomData;

/**
 * A request for {@link CustomData} to be applied to an arbitrary {@link IdentifiableJavaStructure}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1.3
 */
public class GlobalCustomDataRequest implements Iterable<IdentifiableJavaStructure> {
	private Map<IdentifiableJavaStructure, CustomData> customDataRequests = new HashMap<IdentifiableJavaStructure, CustomData>();
	
	public void addCustomData(IdentifiableJavaStructure javaStructure, CustomData customData) {
		customDataRequests.put(javaStructure, customData);
	}

	public CustomData getCustomDataFor(IdentifiableJavaStructure javaStructure) {
		return customDataRequests.get(javaStructure);
	}
	
	public void addAll(GlobalCustomDataRequest otherRequest) {
		for (IdentifiableJavaStructure key : otherRequest) {
			addCustomData(key, otherRequest.getCustomDataFor(key));
		}
	}

	public Iterator<IdentifiableJavaStructure> iterator() {
		return customDataRequests.keySet().iterator();
	}
}
