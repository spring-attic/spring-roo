package org.springframework.roo.addon.layers.service;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.layers.LayerAdapter;
import org.springframework.roo.layers.LayerType;
import org.springframework.roo.layers.Priority;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class ServiceLayerProvider extends LayerAdapter {
	
	@Reference private TypeLocationService typeLocationService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	private static final JavaType ANNOTATION_TYPE = new JavaType(RooService.class.getName());

	public LayerType getLayerType() {
		return LayerType.SERVICE;
	}

	public boolean supports(AnnotationMetadata annotation) {
		return annotation.getAnnotationType().equals(ANNOTATION_TYPE);
	}
	
	public int priority() {
		return Priority.LOW.getNumericValue(); // Lowest priority because it's the default provider.
	}
}
