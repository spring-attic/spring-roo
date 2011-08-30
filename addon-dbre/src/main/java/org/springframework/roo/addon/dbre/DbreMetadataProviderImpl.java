package org.springframework.roo.addon.dbre;

import static org.springframework.roo.model.RooJavaType.ROO_DB_MANAGED;

import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Implementation of  {@link DbreMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class DbreMetadataProviderImpl extends AbstractItdMetadataProvider implements DbreMetadataProvider {
	
	// Fields
	@Reference private DbreModelService dbreModelService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_DB_MANAGED);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_DB_MANAGED);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DbreMetadata.createIdentifier(javaType, path);
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DbreMetadata.getJavaType(metadataIdentificationString);
		Path path = DbreMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		DbManagedAnnotationValues annotationValues = new DbManagedAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		// Abort if the database couldn't be deserialized. This can occur if the DBRE XML file has been deleted or is empty.
		Database database = dbreModelService.getDatabase(false);
		if (database == null) {
			return null;
		}

		// We know governor type details are non-null and can be safely cast
		JavaType javaType = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();
		IdentifierHolder identifierHolder = getIdentifierHolder(javaType);
		if (identifierHolder == null) {
			return null;
		}

		FieldMetadata versionField = getVersionField(javaType, metadataIdentificationString);

		// Search for database-managed entities
		Set<ClassOrInterfaceTypeDetails> managedEntities = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_DB_MANAGED);

		boolean found = false;
		for (ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
			if (managedEntity.getName().equals(javaType)) {
				found = true;
				break;
			}
		}
		if (!found) {
			String mid = typeLocationService.findIdentifier(javaType);
			metadataDependencyRegistry.registerDependency(mid, metadataIdentificationString);
			return null;
		}

		DbreMetadata dbreMetadata = new DbreMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, identifierHolder, versionField, managedEntities, database);
		ClassOrInterfaceTypeDetails updatedGovernor = dbreMetadata.getUpdatedGovernor();
		if (updatedGovernor != null) {
			typeManagementService.createOrUpdateTypeOnDisk(updatedGovernor);
		}
		return dbreMetadata;
	}

	private IdentifierHolder getIdentifierHolder(JavaType javaType) {
		List<FieldMetadata> identifierFields = persistenceMemberLocator.getIdentifierFields(javaType);
		if (identifierFields.isEmpty()) {
			return null;
		}
		
		FieldMetadata identifierField = identifierFields.get(0);
		boolean embeddedIdField = identifierField.getCustomData().get(PersistenceCustomDataKeys.EMBEDDED_ID_FIELD) != null;
		List<FieldMetadata> embeddedIdFields = persistenceMemberLocator.getEmbeddedIdentifierFields(javaType);
		return new IdentifierHolder(identifierField, embeddedIdField, embeddedIdFields);
	}

	private FieldMetadata getVersionField(JavaType domainType, String metadataIdentificationString) {
		return persistenceMemberLocator.getVersionField(domainType);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DbManaged";
	}

	public String getProvidesType() {
		return DbreMetadata.getMetadataIdentiferType();
	}
}