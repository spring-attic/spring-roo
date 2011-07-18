package org.springframework.roo.addon.layers.service;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * The {@link org.springframework.roo.project.layers.LayerProvider} that
 * provides an application's service layer.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class ServiceLayerProvider extends CoreLayerProvider {
	
	// Constants
	private static final JavaType AUTOWIRED = new JavaType("org.springframework.beans.factory.annotation.Autowired");
	// -- Method names; callers will usually use the PersistenceCustomDataKeys enum instead, to avoid a dependency upon this addon
	static final String FIND_ALL_METHOD = PersistenceCustomDataKeys.FIND_ALL_METHOD.name();
	static final String SAVE_METHOD = PersistenceCustomDataKeys.PERSIST_METHOD.name();
	static final String UPDATE_METHOD = PersistenceCustomDataKeys.MERGE_METHOD.name();
	
	// Fields
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private ServiceAnnotationValuesFactory serviceAnnotationValuesFactory;
	@Reference private ServiceInterfaceLocator serviceInterfaceLocator;
	
	public MemberTypeAdditions getMemberTypeAdditions(final String callerMID, final String methodIdentifier, final JavaType targetEntity, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		Assert.isTrue(StringUtils.hasText(callerMID), "Caller's metadata identifier required");
		Assert.notNull(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target entity type required");
		Assert.notNull(methodParameters, "Method param names and types required (may be empty)");
		
		// Check the entity has a plural form
		final String pluralId = PluralMetadata.createIdentifier(targetEntity);
		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
		if (pluralMetadata == null || pluralMetadata.getPlural() == null) {
			return null;
		}
		
		// Loop through the service interfaces that claim to support the given target entity
		for (final ClassOrInterfaceTypeDetails serviceInterface : serviceInterfaceLocator.getServiceInterfaces(targetEntity)) {
			// Get the values of the @RooService annotation for this service interface
			final ServiceAnnotationValues annotationValues = serviceAnnotationValuesFactory.getInstance(serviceInterface);
			if (annotationValues != null) {
				final MemberTypeAdditions methodAdditions = getMethodAdditions(callerMID, methodIdentifier, targetEntity, serviceInterface.getName(), annotationValues, pluralMetadata.getPlural(), methodParameters);
				
				if (methodAdditions != null) {
					// Register the caller for updates of this service
					metadataDependencyRegistry.registerDependency(serviceInterface.getDeclaredByMetadataId(), callerMID);
					
					// Register the caller for updates of the pluralisation of the entity
					metadataDependencyRegistry.registerDependency(pluralId, callerMID);
					
					// Return these additions
					return methodAdditions;
				}
			}
		}
		
		// None of the services for this entity were able to provide the method
		return null;
	}
	
	/**
	 * Returns the additions the caller should make in order to invoke the given
	 * method for the given domain entity.
	 * 
	 * @param callerMID the caller's metadata ID (required)
	 * @param methodIdentifier the internal ID of the method being invoked
	 * @param targetEntity the type of entity being operated upon (required)
	 * @param serviceInterface the domain service type (required)
	 * @param annotationValues the values of the {@link RooService} annotation
	 * on the given service interface (required)
	 * @param plural
	 * @param methodParameters
	 * @return <code>null</code> if that method is not supported by this layer
	 */
	private MemberTypeAdditions getMethodAdditions(final String callerMID, final String methodIdentifier, final JavaType targetEntity, final JavaType serviceInterface, final ServiceAnnotationValues annotationValues, final String plural, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (FIND_ALL_METHOD.equals(methodIdentifier)) {
			return getMethodAdditions(callerMID, serviceInterface, annotationValues.getFindAllMethod(), plural, Arrays.<JavaType>asList(), methodParameters);
		} else if (SAVE_METHOD.equals(methodIdentifier)) {
			return getMethodAdditions(callerMID, serviceInterface, annotationValues.getSaveMethod(), targetEntity.getSimpleTypeName(), Arrays.asList(targetEntity), methodParameters);
		} else if (UPDATE_METHOD.equals(methodIdentifier)) {
			return getMethodAdditions(callerMID, serviceInterface, annotationValues.getUpdateMethod(), targetEntity.getSimpleTypeName(), Arrays.asList(targetEntity), methodParameters);
		}
		return null;
	}
	
	/**
	 * Returns the additions that the caller needs to make in order to invoke
	 * the given method
	 * 
	 * @param callerMID the caller's metadata ID (required)
	 * @param serviceInterface the type of the service interface being queried (required)
	 * @param methodName the name of the method to be invoked (as provided via
	 * the {@link RooService} annotation); can be blank
	 * @param methodSuffix any suffix to be appended to the method name; can be
	 * blank for none
	 * @param parameterTypes the types of parameters taken by services
	 * implementing this method, if any (can be empty but not null)
	 * @param callerParameters the types and names of the parameters provided by
	 * the code calling the method
	 * @return <code>null</code> if the given service doesn't implement a method
	 * with this name and parameter types
	 */
	private MemberTypeAdditions getMethodAdditions(final String callerMID, final JavaType serviceInterface, final String methodName, final String methodSuffix, final List<JavaType> parameterTypes, final Pair<JavaType, JavaSymbolName>... callerParameters) {
		if (!StringUtils.hasText(methodName)) {
			// The service annotation doesn't provide a name for this method, so it's not implemented
			return null;
		}
		if (parameterTypes.size() != callerParameters.length) {
			// The caller has a different number of parameters to this method
			return null;
		}
		for (int i = 0; i < callerParameters.length; i++) {
			if (!parameterTypes.get(i).equals(callerParameters[i].getKey())) {
				// The caller's parameter types don't match this method's parameter types
				return null;
			}
		}
		
		// The method is supported by this service interface; make a builder
		final ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(callerMID);
		
		// Add an autowired field of the type of this service
		final AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		final String fieldName = StringUtils.uncapitalize(serviceInterface.getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(callerMID, 0, Arrays.asList(annotation), new JavaSymbolName(fieldName), serviceInterface).build());
		
		// Generate an additions object that includes a call to the method
		final JavaSymbolName[] parameterNames = new JavaSymbolName[callerParameters.length];
		for (int i = 0; i < callerParameters.length; i++) {
			parameterNames[i] = callerParameters[i].getValue();
		}
		return new MemberTypeAdditions(classBuilder, fieldName, methodName + StringUtils.trimToEmpty(methodSuffix), parameterNames);		
	}
	
	public int getLayerPosition() {
		return LayerType.SERVICE.getPosition();
	}
	
	// Setters for use by unit tests

	void setMetadataDependencyRegistry(final MetadataDependencyRegistry metadataDependencyRegistry) {
		this.metadataDependencyRegistry = metadataDependencyRegistry;
	}
	
	void setMetadataService(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}
	
	void setServiceAnnotationValuesFactory(final ServiceAnnotationValuesFactory serviceAnnotationValuesFactory) {
		this.serviceAnnotationValuesFactory = serviceAnnotationValuesFactory;
	}
	
	void setServiceInterfaceLocator(final ServiceInterfaceLocator serviceInterfaceLocator) {
		this.serviceInterfaceLocator = serviceInterfaceLocator;
	}
}
