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
	
	// Fields
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;

	public List<FieldMetadata> getEmbeddedIdentifierFields(final JavaType domainType) {
		return getEmbeddedIdentifierFields(getMemberDetails(domainType));
	}

	public List<FieldMetadata> getEmbeddedIdentifierFields(final MemberDetails memberDetails) {
		final List<FieldMetadata> compositePkFields = new ArrayList<FieldMetadata>();
		if (memberDetails == null) {
			return compositePkFields;
		}
		
		final List<FieldMetadata> embeddedFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (embeddedFields.isEmpty()) {
			return embeddedFields;
		}
		final List<FieldMetadata> fields = MemberFindingUtils.getFields(getMemberDetails(embeddedFields.get(0).getFieldType()));
		for (final FieldMetadata field : fields) {
			if (!field.getCustomData().keySet().contains("SERIAL_VERSION_UUID_FIELD")) {
				compositePkFields.add(field);
			}
		}
		return compositePkFields;
	}
	
	public MethodMetadata getIdentifierAccessor(final JavaType domainType) {
		return getIdentifierAccessor(getPhysicalTypeMetadata(domainType));
	}
	
	public MethodMetadata getIdentifierAccessor(final PhysicalTypeMetadata physicalTypeMetadata) {
		if (physicalTypeMetadata == null || !physicalTypeMetadata.isValid()) {
			return null;
		}
		return getIdentifierAccessor(getMemberDetails(physicalTypeMetadata));
	}
	
	public MethodMetadata getIdentifierAccessor(final MemberDetails domainType) {
		return MemberFindingUtils.getMostConcreteMethodWithTag(domainType, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
	}

	public List<FieldMetadata> getIdentifierFields(final JavaType domainType) {
		return getIdentifierFields(getMemberDetails(domainType));
	}

	public List<FieldMetadata> getIdentifierFields(final MemberDetails memberDetails) {
		final List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		if (idFields.isEmpty()) {
			// Return the embedded ID fields, if any
			return MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		}
		return idFields;
	}

	/**
	 * Returns the {@link MemberDetails} for the given {@link JavaType}
	 * 
	 * @param javaType (can be <code>null</code>)
	 * @return <code>null</code> if they can't be found
	 */
	private MemberDetails getMemberDetails(final JavaType javaType) {
		return getMemberDetails(getPhysicalTypeMetadata(javaType));
	}
	
	/**
	 * Returns the {@link MemberDetails} for the given {@link PhysicalTypeMetadata}
	 * 
	 * @param physicalTypeMetadata can be <code>null</code>
	 * @return <code>null</code> if the details can't be found
	 */
	private MemberDetails getMemberDetails(final PhysicalTypeMetadata physicalTypeMetadata) {
		if (physicalTypeMetadata == null || !physicalTypeMetadata.isValid()) {
			return null;
		}
		final ClassOrInterfaceTypeDetails coitd = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		return memberDetailsScanner.getMemberDetails(getClass().getName(), coitd);
	}

	/**
	 * Returns the {@link PhysicalTypeMetadata} for the given {@link JavaType}
	 * 
	 * @param javaType can be <code>null</code>
	 * @return <code>null</code> if the given {@link JavaType} is <code>null</code>
	 */
	private PhysicalTypeMetadata getPhysicalTypeMetadata(final JavaType javaType) {
		if (javaType == null) {
			return null;
		}
		return (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType));
	}

	public MethodMetadata getVersionAccessor(final JavaType domainType) {
		return getVersionAccessor(getMemberDetails(domainType));
	}

	public MethodMetadata getVersionAccessor(final MemberDetails domainType) {
		return MemberFindingUtils.getMostConcreteMethodWithTag(domainType, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
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

	public List<FieldMetadata> getVersionFields(final MemberDetails domainType) {
		return MemberFindingUtils.getFieldsWithTag(domainType, PersistenceCustomDataKeys.VERSION_FIELD);
	}
}
