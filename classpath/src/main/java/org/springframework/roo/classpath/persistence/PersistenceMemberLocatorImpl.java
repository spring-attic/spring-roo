package org.springframework.roo.classpath.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

/**
 * This implementation of {@link PersistenceMemberLocator} scans for the presence of 
 * persistence ID tags for {@link MemberDetails} for a given domain type.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class PersistenceMemberLocatorImpl implements PersistenceMemberLocator {
	
	// Fields
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private FileMonitorService fileMonitorService;
	@Reference private TypeLocationService typeLocationService;

	private final Map<JavaType, List<FieldMetadata>> domainTypeIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
	private final Map<JavaType, JavaType> domainTypeIdCache = new HashMap<JavaType, JavaType>();
	private final Map<JavaType, List<FieldMetadata>> domainTypeEmbeddedIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
	private final Map<JavaType, FieldMetadata> domainTypeVersionFieldCache = new HashMap<JavaType, FieldMetadata>();
	private final Map<JavaType, MethodMetadata> domainTypeIdAccessorCache = new HashMap<JavaType, MethodMetadata>();
	private final Map<JavaType, MethodMetadata> domainTypeVersionAccessorCache = new HashMap<JavaType, MethodMetadata>();

	public List<FieldMetadata> getEmbeddedIdentifierFields(final JavaType domainType) {
		updateCache(domainType);
		if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
			return new ArrayList<FieldMetadata>(domainTypeEmbeddedIdFieldsCache.get(domainType));
		}
		return new ArrayList<FieldMetadata>();
	}
	
	public JavaType getIdentifierType(JavaType domainType) {
		updateCache(domainType);
		if (domainTypeIdCache.containsKey(domainType)) {
			return domainTypeIdCache.get(domainType);
		}
		return null;
	}
	
	public MethodMetadata getIdentifierAccessor(final JavaType domainType) {
		updateCache(domainType);
		return domainTypeIdAccessorCache.get(domainType);
	}
	
	public List<FieldMetadata> getIdentifierFields(final JavaType domainType) {
		updateCache(domainType);
		if (domainTypeIdFieldsCache.containsKey(domainType)) {
			return new ArrayList<FieldMetadata>(domainTypeIdFieldsCache.get(domainType));
		} else if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
			return new ArrayList<FieldMetadata>(domainTypeEmbeddedIdFieldsCache.get(domainType));
		}

		return new ArrayList<FieldMetadata>();
	}
	
	public MethodMetadata getVersionAccessor(final JavaType domainType) {
		updateCache(domainType);
		return domainTypeVersionAccessorCache.get(domainType);
	}

	public FieldMetadata getVersionField(JavaType domainType) {
		updateCache(domainType);
		return domainTypeVersionFieldCache.get(domainType);
	}

	private Set<String> changeSet = new HashSet<String>();

	private boolean hasRelevantFilesChange(JavaType javaType) {

		String mid = typeLocationService.findIdentifier(javaType);
		if (!PhysicalTypeIdentifier.isValid(mid)) {
			return false;
		}

		Set<String> changes = typeLocationService.getWhatsDirty(getClass().getName());
		changeSet.addAll(changes);
		Set<String> toRemove = new HashSet<String>();
		boolean updateCache = false;
		for (String change : changeSet) {
			JavaType changedType = PhysicalTypeIdentifier.getJavaType(change);
			if (!javaType.equals(changedType)) {
				continue;
			}
			toRemove.add(change);
			updateCache = true;
		}
		changeSet.removeAll(toRemove);

		return updateCache;
	}

	public void updateCache(JavaType domainType) {
		if (!hasRelevantFilesChange(domainType)) {
			return;
		}

		MemberDetails details = getMemberDetails(domainType);
		
		if (MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(details, PersistenceCustomDataKeys.PERSISTENT_TYPE) == null) {
			return;
		}

		// Update normal persistence ID fields cache
		populateIdTypes(details, domainType);
				
		// Update normal persistence ID cache
		populateIdFields(details, domainType);
		
		// Update embedded ID fields cache
		populateEmbeddedIdFields(details, domainType);
		
		// Update ID accessor cache
		populateIdAccessors(details, domainType);

		// Update version field cache
		populateVersionField(details, domainType);

		// Update version accessor cache
		populateVersionAccessor(details, domainType);

	}

	private void populateVersionAccessor(MemberDetails details, JavaType type) {
		MethodMetadata versionAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(details, PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD);
		if (versionAccessor != null) {
			domainTypeVersionAccessorCache.put(type, versionAccessor);
		} else if (domainTypeVersionAccessorCache.containsKey(type)) {
			domainTypeVersionAccessorCache.remove(type);
		}
	}

	private void populateIdAccessors(MemberDetails details, JavaType type) {
		MethodMetadata idAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(details, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		if (idAccessor != null) {
			domainTypeIdAccessorCache.put(type, idAccessor);
		} else if (domainTypeIdAccessorCache.containsKey(type)) {
			domainTypeIdAccessorCache.remove(type);
		}
	}

	private void populateEmbeddedIdFields(MemberDetails details, JavaType type) {
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (!embeddedIdFields.isEmpty()) {
			domainTypeEmbeddedIdFieldsCache.remove(type);
			domainTypeEmbeddedIdFieldsCache.put(type, new ArrayList<FieldMetadata>());
			final List<FieldMetadata> fields = MemberFindingUtils.getFields(getMemberDetails(embeddedIdFields.get(0).getFieldType()));
			for (final FieldMetadata field : fields) {
				if (!field.getCustomData().keySet().contains("SERIAL_VERSION_UUID_FIELD")) {
					domainTypeEmbeddedIdFieldsCache.get(type).add(field);
				}
			}
		} else if (domainTypeEmbeddedIdFieldsCache.containsKey(type)) {
			domainTypeEmbeddedIdFieldsCache.remove(type);
		}
	}

	private void populateIdTypes(MemberDetails details, JavaType type) {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (!idFields.isEmpty()) {
			domainTypeIdCache.put(type, idFields.get(0).getFieldType());
		} else if (!embeddedIdFields.isEmpty()) {
			domainTypeIdCache.put(type, embeddedIdFields.get(0).getFieldType());
		} else if (domainTypeIdCache.containsKey(type)) {
			domainTypeIdCache.remove(type);
		}
	}
	
	private void populateIdFields(MemberDetails details, JavaType type) {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.IDENTIFIER_FIELD);
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (!idFields.isEmpty()) {
			domainTypeIdFieldsCache.put(type, idFields);
		} else if (!embeddedIdFields.isEmpty()) {
			domainTypeIdFieldsCache.put(type, embeddedIdFields);
		} else if (domainTypeIdFieldsCache.containsKey(type)) {
			domainTypeIdFieldsCache.remove(type);
		}
	}

	private void populateVersionField(MemberDetails details, JavaType type) {
		List<FieldMetadata> versionFields = MemberFindingUtils.getFieldsWithTag(details, PersistenceCustomDataKeys.VERSION_FIELD);
		if (!versionFields.isEmpty()) {
			domainTypeVersionFieldCache.put(type, versionFields.get(0));
		} else if (domainTypeVersionFieldCache.containsKey(type)) {
			domainTypeVersionFieldCache.remove(type);
		}
	}

	private MemberDetails getMemberDetails(final JavaType type) {
		final ClassOrInterfaceTypeDetails physicalTypeMetadata = typeLocationService.getTypeForIdentifier(PhysicalTypeIdentifier.createIdentifier(type));
		if (physicalTypeMetadata == null ) {
			return null;
		}
		return memberDetailsScanner.getMemberDetails(getClass().getName(),  physicalTypeMetadata);
	}
}
