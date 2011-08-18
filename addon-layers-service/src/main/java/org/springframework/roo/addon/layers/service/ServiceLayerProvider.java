package org.springframework.roo.addon.layers.service;

import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.layers.CoreLayerProvider;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.PairList;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * The {@link org.springframework.roo.classpath.layers.LayerProvider} that
 * provides an application's service layer.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ServiceLayerProvider extends CoreLayerProvider {
	
	// Fields
	@Reference private MetadataService metadataService;
	@Reference private ServiceAnnotationValuesFactory serviceAnnotationValuesFactory;
	@Reference private ServiceInterfaceLocator serviceInterfaceLocator;
	
	public MemberTypeAdditions getMemberTypeAdditions(final String callerMID, final String methodIdentifier, final JavaType targetEntity, final JavaType idType, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		Assert.isTrue(StringUtils.hasText(callerMID), "Caller's metadata identifier required");
		Assert.notNull(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target entity type required");
		Assert.notNull(methodParameters, "Method param names and types required (may be empty)");

		// Check whether this is even a known service layer method
		final PairList<JavaType, JavaSymbolName> parameterList = new PairList<JavaType, JavaSymbolName>(methodParameters);
		final List<JavaType> parameterTypes = parameterList.getKeys();
		final ServiceLayerMethod method = ServiceLayerMethod.valueOf(methodIdentifier, parameterTypes, targetEntity, idType);
		if (method == null) {
			return null;
		}
		
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
				// Check whether this method is implemented by the given service
				final String methodName = method.getName(annotationValues, targetEntity, pluralMetadata.getPlural());
				if (StringUtils.hasText(methodName)) {
					// The service implements the method; get the additions to be made by the caller
					final MemberTypeAdditions methodAdditions = getMethodAdditions(callerMID, methodName, serviceInterface.getName(), parameterList.getValues());
					
					// Register the caller for updates of this service
//					metadataDependencyRegistry.registerDependency(serviceInterface.getDeclaredByMetadataId(), callerMID);
					
					// Register the caller for updates of the pluralisation of the entity
//					metadataDependencyRegistry.registerDependency(pluralId, callerMID);
					
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
	 * @param methodName the name of the method being invoked (required)
	 * @param serviceInterface the domain service type (required)
	 * @param parameterNames the names of the parameters being passed by the
	 * caller to the method
	 * @return a non-<code>null</code> set of additions
	 */
	private MemberTypeAdditions getMethodAdditions(final String callerMID, final String methodName, final JavaType serviceInterface, final List<JavaSymbolName> parameterNames) {
		
		// The method is supported by this service interface; make a builder
		final ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(callerMID);
		
		// Add an autowired field of the type of this service
		final AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		final String fieldName = StringUtils.uncapitalize(serviceInterface.getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(callerMID, 0, Arrays.asList(annotation), new JavaSymbolName(fieldName), serviceInterface).build());
		
		// Generate an additions object that includes a call to the method
		return MemberTypeAdditions.getInstance(classBuilder, fieldName, methodName, parameterNames);		
	}
	
	public int getLayerPosition() {
		return LayerType.SERVICE.getPosition();
	}
	
	// -------------------- Setters for use by unit tests ----------------------
	
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
