package org.springframework.roo.addon.dod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.beaninfo.BeanInfoUtils;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link DataOnDemandMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class DataOnDemandMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	private Map<JavaType, String> entityToDodMidMap = new HashMap<JavaType, String>();
	private Map<String, JavaType> dodMidToEntityMap = new HashMap<String, JavaType>();
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// DOD classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
		addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any DOD metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		String localMid = entityToDodMidMap.get(governor);
		if (localMid == null) {
			// No DOD is not interested in this JavaType, so let's move on
			return null;
		}
		
		// We have some DOD metadata, so let's check if we care if any methods match our requirements
		for (MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
			if (isMethodOfInterest(method)) {
				// A DOD cares about the JavaType, and an ITD offers a method likely of interest, so let's formally trigger it to run.
				// Note that it will re-scan and discover this ITD, and register a direct dependency on it for the future.
				return localMid;
			}
		}
		
		return null;
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// We need to parse the annotation, which we expect to be present
		DataOnDemandAnnotationValues annotationValues = new DataOnDemandAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getEntity() == null) {
			return null;
		}
		
		// Remember that this entity JavaType matches up with this DOD's metadata identification string
		// Start by clearing the previous association
		JavaType oldEntity = dodMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToDodMidMap.remove(oldEntity);
		}
		entityToDodMidMap.put(annotationValues.getEntity(), metadataIdentificationString);
		dodMidToEntityMap.put(metadataIdentificationString, annotationValues.getEntity());
		
		// Lookup the entity's metadata
		JavaType entityJavaType = annotationValues.getEntity();
		Path entityPath = Path.SRC_MAIN_JAVA;
		String entityMetadataKey = EntityMetadata.createIdentifier(entityJavaType, entityPath);
		String entityClassOrInterfaceMetadataKey = PhysicalTypeIdentifier.createIdentifier(entityJavaType, entityPath);
		
		// We need to lookup the metadata we depend on
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		PhysicalTypeMetadata entityPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(entityClassOrInterfaceMetadataKey);

		// We need to abort if we couldn't find dependent metadata
		if (entityMetadata == null || !entityMetadata.isValid() || entityPhysicalTypeMetadata == null) {
			return null;
		}
		
		ClassOrInterfaceTypeDetails entityClassOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) entityPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		
		// Abort if the entity's class details aren't available (parse error etc)
		if (entityClassOrInterfaceTypeDetails == null) {
			return null;
		}
		
		// Identify all the setters we care about on the entity
		Map<MethodMetadata, FieldMetadata> locatedSetters = new LinkedHashMap<MethodMetadata, FieldMetadata>();
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), entityClassOrInterfaceTypeDetails);
		for (MemberHoldingTypeDetails memberHolder : memberDetails.getDetails()) {
			// avoid BIM; deprecating
			if (memberHolder.getDeclaredByMetadataId().startsWith("MID:org.springframework.roo.addon.beaninfo.BeanInfoMetadata#")) continue;
			
			// Add the methods we care to the locatedSetters
			for (MethodMetadata method : memberHolder.getDeclaredMethods()) {
				if (isMethodOfInterest(method)) {
					JavaSymbolName propertyName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
					FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, propertyName);
					if (field == null) continue;
					locatedSetters.put(method, field);
					// Track any changes to that method (eg it goes away)
					metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
				}
			}
		}
		
		MethodMetadata findEntriesMethod = entityMetadata.getFindEntriesMethod();
		if (findEntriesMethod == null) {
			return null;
		}
		
		MethodMetadata persistMethod = entityMetadata.getPersistMethod();
		MethodMetadata flushMethod = entityMetadata.getFlushMethod();
		MethodMetadata findMethod = entityMetadata.getFindMethod();
		MethodMetadata identifierAccessor = entityMetadata.getIdentifierAccessor();
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(entityClassOrInterfaceMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		
		return new DataOnDemandMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, identifierAccessor, findMethod, findEntriesMethod, persistMethod, flushMethod, metadataService, metadataDependencyRegistry, locatedSetters, entityClassOrInterfaceTypeDetails);
	}

	private boolean isMethodOfInterest(MethodMetadata method) {
		return method.getMethodName().getSymbolName().startsWith("set");
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "DataOnDemand";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DataOnDemandMetadata.getJavaType(metadataIdentificationString);
		Path path = DataOnDemandMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DataOnDemandMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return DataOnDemandMetadata.getMetadataIdentiferType();
	}
}