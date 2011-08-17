package org.springframework.roo.classpath.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

/**
 * This implementation of {@link PersistenceMemberLocator} scans for the presence of 
 * persistence ID tags for {@link MemberDetails} for a given domain type.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 *
 */
@Component(immediate=true)
@Service
public class PersistenceMemberLocatorImpl implements PersistenceMemberLocator, MetadataNotificationListener {
	
	// Fields
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	
	private final Map<JavaType, List<FieldMetadata>> domainTypeIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
	private final Map<JavaType, JavaType> domainTypeIdCache = new HashMap<JavaType, JavaType>();
	private final Map<JavaType, List<FieldMetadata>> domainTypeEmbeddedIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
	private final Map<JavaType, FieldMetadata> domainTypeVersionFieldCache = new HashMap<JavaType, FieldMetadata>();
	private final Map<JavaType, MethodMetadata> domainTypeIdAccessorCache = new HashMap<JavaType, MethodMetadata>();
	private final Map<JavaType, MethodMetadata> domainTypeVersionAccessorCache = new HashMap<JavaType, MethodMetadata>();
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public List<FieldMetadata> getEmbeddedIdentifierFields(final JavaType domainType) {
		if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
			return domainTypeEmbeddedIdFieldsCache.get(domainType);
		}
		return new ArrayList<FieldMetadata>();
	}
	
	public JavaType getIdentifierType(JavaType domainType) {
		notify(PhysicalTypeIdentifier.createIdentifier(domainType), null);
		if (domainTypeIdCache.containsKey(domainType)) {
			return domainTypeIdCache.get(domainType);
		}
		return null;
	}
	
	public MethodMetadata getIdentifierAccessor(final JavaType domainType) {
		return domainTypeIdAccessorCache.get(domainType);
	}
	
	public List<FieldMetadata> getIdentifierFields(final JavaType domainType) {
		// It is possible that the notify method has not been called yet for this type, let's try now.
		notify(PhysicalTypeIdentifier.createIdentifier(domainType), null);
		if (domainTypeIdFieldsCache.containsKey(domainType)) {
			return domainTypeIdFieldsCache.get(domainType);
		} else if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
			return domainTypeEmbeddedIdFieldsCache.get(domainType);
		}
		return new ArrayList<FieldMetadata>();
	}
	
	public MethodMetadata getVersionAccessor(final JavaType domainType) {
		return domainTypeVersionAccessorCache.get(domainType);
	}

	public FieldMetadata getVersionField(JavaType domainType) {
		return domainTypeVersionFieldCache.get(domainType);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (!PhysicalTypeIdentifier.isValid(upstreamDependency)) {
			return;
		}
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(upstreamDependency);
		if (physicalTypeMetadata == null) {
			return;
		}
		MemberHoldingTypeDetails memberHoldingTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (memberHoldingTypeDetails == null || !(memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails)) {
			return;
		}
		MemberDetails details = memberDetailsScanner.getMemberDetails(getClass().getName(), (ClassOrInterfaceTypeDetails) memberHoldingTypeDetails);
		
		if (MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(details, PersistenceCustomDataKeys.PERSISTENT_TYPE) == null) {
			return;
		}
		JavaType type = memberHoldingTypeDetails.getName();
		
		// Get normal persistence ID fields
		populateIdTypes(details, type);
				
		// Get normal persistence ID fields
		populateIdFields(details, type);
		
		// Get embedded ID fields 
		populateEmbeddedIdFields(details, type);
		
		// Get ID accessor
		populateIdAccessors(details, type);
		
		// Get version accessor
		populateVersionAccessor(details, type);
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

	private MemberDetails getMemberDetails(final JavaType type) {
		final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type));
		if (physicalTypeMetadata == null || !(physicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
			return null;
		}
		return memberDetailsScanner.getMemberDetails(getClass().getName(), (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails());
	}
}
