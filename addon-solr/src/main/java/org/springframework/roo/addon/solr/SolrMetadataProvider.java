package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.RooJavaType.ROO_SOLR_SEARCHABLE;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.EntityMetadataProvider;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link SolrMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public final class SolrMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {
	
	// Fields
	@Reference private EntityMetadataProvider entityMetadataProvider;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		entityMetadataProvider.addMetadataTrigger(ROO_SOLR_SEARCHABLE);
		addMetadataTrigger(ROO_SOLR_SEARCHABLE);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		entityMetadataProvider.removeMetadataTrigger(ROO_SOLR_SEARCHABLE);
		removeMetadataTrigger(ROO_SOLR_SEARCHABLE);	
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		SolrSearchAnnotationValues annotationValues = new SolrSearchAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.searchMethod == null) {
			return null;
		}
		
		// Acquire bean info (we need getters details, specifically)
		JavaType javaType = SolrMetadata.getJavaType(metadataIdentificationString);
		Path path = SolrMetadata.getPath(metadataIdentificationString);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		
		// We want to be notified if the getter info changes in any way 
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		
		// Abort if we don't have getter information available
		if (entityMetadata == null || !entityMetadata.isValid()) {
			return null;
		}
		
		// Otherwise go off and create the Solr metadata
		String beanPlural = javaType.getSimpleTypeName() + "s";
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(javaType, path));
		if (pluralMetadata != null && pluralMetadata.isValid()) {
			beanPlural = pluralMetadata.getPlural();
		}
		
		MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		Map<MethodMetadata, FieldMetadata> accessorDetails = new LinkedHashMap<MethodMetadata, FieldMetadata>();
		for (MethodMetadata method : memberDetails.getMethods()) {
			if (BeanInfoUtils.isAccessorMethod(method) && !method.getMethodName().getSymbolName().startsWith("is")) {
				FieldMetadata fieldMetadata = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
				if (fieldMetadata != null) {
					accessorDetails.put(method, fieldMetadata);
				}
				// Track any changes to that method (eg it goes away)
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			}
		}
		final MethodMetadata idAccessor = persistenceMemberLocator.getIdentifierAccessor(javaType);
		final FieldMetadata versionField = persistenceMemberLocator.getVersionField(javaType);
		return new SolrMetadata(metadataIdentificationString, aspectName, annotationValues, governorPhysicalTypeMetadata, idAccessor, versionField, accessorDetails, beanPlural);
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine if this ITD presents a method we're interested in (namely accessors)
		for (MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
			if (BeanInfoUtils.isAccessorMethod(method) && !method.getMethodName().getSymbolName().startsWith("is")) {
				// We care about this ITD, so formally request an update so we can scan for it and process it
				
				// Determine the governor for this ITD, and the Path the ITD is stored within
				JavaType governorType = itdTypeDetails.getName();
				String providesType = MetadataIdentificationUtils.getMetadataClass(itdTypeDetails.getDeclaredByMetadataId());
				Path itdPath = PhysicalTypeIdentifierNamingUtils.getPath(providesType, itdTypeDetails.getDeclaredByMetadataId());
				
				//  Produce the local MID we're going to use to make the request
				String localMid = createLocalIdentifier(governorType, itdPath);
				
				// Request the local MID
				return localMid;
			}
		}
		
		return null;
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "SolrSearch";
	}
	
	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = SolrMetadata.getJavaType(metadataIdentificationString);
		Path path = SolrMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
	
	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return SolrMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return SolrMetadata.getMetadataIdentiferType();
	}
}