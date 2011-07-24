package org.springframework.roo.addon.layers.service;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.Filter;

/**
 * Locates interfaces annotated with {@link RooService} that meet certain
 * criteria.
 * 
 * Factored out of {@link ServiceLayerProvider} to simplify unit testing of that
 * class.
 *
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class ServiceInterfaceLocatorImpl implements ServiceInterfaceLocator {
	
	// Constants
	private static final JavaType ROO_SERVICE = new JavaType(RooService.class.getName());
	
	// Fields
	@Reference private TypeLocationService typeLocationService;
	
	/**
	 * Returns the details of any interfaces annotated with {@link RooService}
	 * that claim to support the given type of entity.
	 * 
	 * @param entityType can't be <code>null</code>
	 * @return a non-<code>null</code> collection; empty if there's no such
	 * services or the given entity is <code>null</code>
	 */
	public Collection<ClassOrInterfaceTypeDetails> getServiceInterfaces(final JavaType entityType) {
		Assert.notNull(entityType, "Entity type is required");
		final Iterable<ClassOrInterfaceTypeDetails> allServices = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_SERVICE);
		final Filter<ClassOrInterfaceTypeDetails> entityServicefilter = new Filter<ClassOrInterfaceTypeDetails>() {
			public boolean include(final ClassOrInterfaceTypeDetails serviceInterface) {
				final AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(serviceInterface.getAnnotations(), ROO_SERVICE);
				// Find the domain type(s) supported by this service interface
				@SuppressWarnings("unchecked")
				final ArrayAttributeValue<AnnotationAttributeValue<JavaType>> domainTypes = (ArrayAttributeValue<AnnotationAttributeValue<JavaType>>) annotation.getAttribute(RooService.DOMAIN_TYPES_ATTRIBUTE);
				if (domainTypes != null) {
					// Look through those domain types for the given one
					for (final AnnotationAttributeValue<JavaType> javaTypeValue : domainTypes.getValue()) {
						if (entityType.equals(javaTypeValue.getValue())) {
							return true;
						}
					}
				}
				return false;
			}
		};
		return CollectionUtils.filter(allServices, entityServicefilter);
	}
}