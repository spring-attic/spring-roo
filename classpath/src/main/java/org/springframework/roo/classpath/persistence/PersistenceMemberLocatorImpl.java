package org.springframework.roo.classpath.persistence;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

/**
 * This implementation of {@link PersistenceMemberLocator} scans for the presence of 
 * persistence ID tags for {@link MemberDetails} for a given domain type.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 *
 */
@Component(immediate=true)
@Service
public class PersistenceMemberLocatorImpl implements PersistenceMemberLocator {
	
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;

	public List<FieldMetadata> getIdentifierFields(JavaType domainType) {
		MemberDetails memberDetails = getMemberDetails(domainType);
		if (memberDetails == null) {
			return new ArrayList<FieldMetadata>();
		}
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		if (idFields.isEmpty()) {
			idFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		}
		return idFields;
	}
	
	public List<FieldMetadata> getEmbeddedIdentifierFields(JavaType domainType) {
		MemberDetails memberDetails = getMemberDetails(domainType);
		List<FieldMetadata> compositePkFields = new ArrayList<FieldMetadata>();
		if (memberDetails == null) {
			return compositePkFields;
		}
		
		List<FieldMetadata> embeddedFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (embeddedFields.isEmpty()) {
			return embeddedFields;
		}
		List<FieldMetadata> fields = MemberFindingUtils.getFields(getMemberDetails(embeddedFields.get(0).getFieldType()));
		for (FieldMetadata field: fields) {
			if (!field.getCustomData().keySet().contains("SERIAL_VERSION_UUID_FIELD")) {
				compositePkFields.add(field);
			}
		}
		return compositePkFields;
	}
	
	private MemberDetails getMemberDetails(JavaType javaType) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType));
		if (physicalTypeMetadata == null) {
			return null;
		}
		ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		return memberDetailsScanner.getMemberDetails(getClass().getName(), coitd);
	}

	public MethodMetadata getIdentifierAccessor(final JavaType domainType) {
		if (domainType == null) {
			return null;
		}
		return getIdentifierAccessor((PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(domainType)));
	}

	public MethodMetadata getIdentifierAccessor(final PhysicalTypeMetadata physicalTypeMetadata) {
		if (physicalTypeMetadata == null || !physicalTypeMetadata.isValid()) {
			return null;
		}
		final ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (coitd == null) {
			return null;
		}
		return getIdentifierAccessor(memberDetailsScanner.getMemberDetails(getClass().getName(), coitd));
	}

	public MethodMetadata getIdentifierAccessor(final MemberDetails domainType) {
		return MemberFindingUtils.getMostConcreteMethodWithTag(domainType, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
	}

	public MethodMetadata getVersionAccessor(final MemberDetails domainType) {
		return MemberFindingUtils.getMostConcreteMethodWithTag(domainType, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
	}

	public List<FieldMetadata> getVersionFields(final MemberDetails domainType) {
		return MemberFindingUtils.getFieldsWithTag(domainType, PersistenceCustomDataKeys.VERSION_FIELD);
	}

	public FieldMetadata getVersionField(final MemberDetails domainType) {
		final List<FieldMetadata> versionFields = getVersionFields(domainType);
		switch (versionFields.size()) {
			case 0:
				return null;
			case 1:
				return versionFields.get(0);
			default:
				throw new IllegalStateException("Expected at most one version field, but found:\n" + versionFields);
		}
	}
}
