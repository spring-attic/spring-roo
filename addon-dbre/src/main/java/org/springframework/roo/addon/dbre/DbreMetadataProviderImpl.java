package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dod.DataOnDemandMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataPersistenceTags;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link DbreMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class DbreMetadataProviderImpl extends AbstractItdMetadataProvider implements DbreMetadataProvider {
	@Reference private DbreModelService dbreModelService;
	@Reference private TypeLocationService typeLocationService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooDbManaged.class.getName()));
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
		// Abort if the database couldn't be deserialized. This can occur if the DBRE XML file has been deleted or is empty.
		Database database = dbreModelService.getDatabaseFromCache();
		if (database == null) {
			return null;
		}

		// We know governor type details are non-null and can be safely cast
		JavaType javaType = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();

		MemberDetails memberDetails = getMemberDetails(javaType);
		if (memberDetails == null) {
			return null;
		}

		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, CustomDataPersistenceTags.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		List<? extends FieldMetadata> entityFields = persistenceMemberHoldingTypeDetails.getDeclaredFields();
		List<? extends MethodMetadata> entityMethods = persistenceMemberHoldingTypeDetails.getDeclaredMethods();
		FieldMetadata identifierField = getIdentifierField(memberDetails, metadataIdentificationString);
		EmbeddedIdentifierHolder embeddedIdentifierHolder = getEmbeddedIdentifierHolder(memberDetails, metadataIdentificationString);
		FieldMetadata versionField = getVersionField(memberDetails, metadataIdentificationString);

		// Search for database-managed entities
		Set<ClassOrInterfaceTypeDetails> managedEntities = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooDbManaged.class.getName()));

		return new DbreMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, entityFields, entityMethods, identifierField, embeddedIdentifierHolder, versionField, managedEntities, database);
	}

	private FieldMetadata getIdentifierField(MemberDetails memberDetails, String metadataIdentificationString) {
		List<FieldMetadata> identifierFields = MemberFindingUtils.getFieldsWithTag(memberDetails, CustomDataPersistenceTags.IDENTIFIER_FIELD);
		if (!identifierFields.isEmpty()) {
			return identifierFields.get(0);
		}
		return null;
	}

	private EmbeddedIdentifierHolder getEmbeddedIdentifierHolder(MemberDetails memberDetails, String metadataIdentificationString) {
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(memberDetails, CustomDataPersistenceTags.EMBEDDED_ID_FIELD);
		if (!embeddedIdFields.isEmpty()) {
			FieldMetadata embeddedIdentifierField = embeddedIdFields.get(0);
			MemberDetails identifierMemberDetails = getMemberDetails(embeddedIdentifierField.getFieldType());
			if (identifierMemberDetails != null) {
				MemberHoldingTypeDetails identifierMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(identifierMemberDetails, CustomDataPersistenceTags.IDENTIFIER_TYPE);
				if (identifierMemberHoldingTypeDetails != null) {
					List<FieldMetadata> identifierFields = new LinkedList<FieldMetadata>();
					for (FieldMetadata field : MemberFindingUtils.getFields(identifierMemberDetails)) {
						if (!(Modifier.isStatic(field.getModifier()) || Modifier.isFinal(field.getModifier()) || Modifier.isTransient(field.getModifier()))) {
							metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
							identifierFields.add(field);
						}
					}
					return new EmbeddedIdentifierHolder(embeddedIdentifierField, identifierFields);
				}
			}
		}
		return null;
	}

	private FieldMetadata getVersionField(MemberDetails memberDetails, String metadataIdentificationString) {
		FieldMetadata versionField = null;
		List<FieldMetadata> fields = MemberFindingUtils.getFieldsWithTag(memberDetails, CustomDataPersistenceTags.VERSION_FIELD);
		if (!fields.isEmpty()) {
			versionField = fields.get(0);
			metadataDependencyRegistry.registerDependency(versionField.getDeclaredByMetadataId(), metadataIdentificationString);
		}
		return versionField;
	}

	private MemberDetails getMemberDetails(JavaType type) {
		// We need to lookup the metadata we depend on
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));

		// We need to abort if we couldn't find dependent metadata
		if (physicalTypeMetadata == null || !physicalTypeMetadata.isValid()) {
			return null;
		}

		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (classOrInterfaceTypeDetails == null) {
			// Abort if the type's class details aren't available (parse error etc)
			return null;
		}

		return memberDetailsScanner.getMemberDetails(DataOnDemandMetadataProvider.class.getName(), classOrInterfaceTypeDetails);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DbManaged";
	}

	public String getProvidesType() {
		return DbreMetadata.getMetadataIdentiferType();
	}
}