package org.springframework.roo.addon.jsf;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link JsfManagedBeanMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfManagedBeanMetadataProviderImpl extends AbstractItdMetadataProvider implements JsfManagedBeanMetadataProvider {

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJsfManagedBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJsfManagedBean.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		JsfAnnotationValues annotationValues = new JsfAnnotationValues(governorPhysicalTypeMetadata);
		JavaType entityType = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entityType == null) {
			return null;
		}

		// Lookup the form backing object's metadata
		MemberDetails memberDetails = getMemberDetails(entityType);
		if (memberDetails == null) {
			return null;
		}

		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}
		
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entityType.getSimpleTypeName() + "'");
		String plural = pluralMetadata.getPlural();

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		return new JsfManagedBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, plural);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ManagedBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JsfManagedBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JsfManagedBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JsfManagedBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfManagedBeanMetadata.getMetadataIdentiferType();
	}
}