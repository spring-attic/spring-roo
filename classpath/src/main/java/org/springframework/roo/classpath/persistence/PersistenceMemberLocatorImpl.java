package org.springframework.roo.classpath.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.ItdDiscoveryService;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaType;

/**
 * This implementation of {@link PersistenceMemberLocator} scans for the
 * presence of persistence ID tags for {@link MemberDetails} for a given domain
 * type.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class PersistenceMemberLocatorImpl implements PersistenceMemberLocator {

    @Reference private ItdDiscoveryService itdDiscoveryService;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private TypeLocationService typeLocationService;

    private final Map<JavaType, List<FieldMetadata>> domainTypeEmbeddedIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
    private final Map<JavaType, MethodMetadata> domainTypeIdAccessorCache = new HashMap<JavaType, MethodMetadata>();
    private final Map<JavaType, JavaType> domainTypeIdCache = new HashMap<JavaType, JavaType>();
    private final Map<JavaType, List<FieldMetadata>> domainTypeIdFieldsCache = new HashMap<JavaType, List<FieldMetadata>>();
    private final Map<JavaType, MethodMetadata> domainTypeVersionAccessorCache = new HashMap<JavaType, MethodMetadata>();
    private final Map<JavaType, FieldMetadata> domainTypeVersionFieldCache = new HashMap<JavaType, FieldMetadata>();

    public List<FieldMetadata> getEmbeddedIdentifierFields(
            final JavaType domainType) {
        updateCache(domainType);
        if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
            return new ArrayList<FieldMetadata>(
                    domainTypeEmbeddedIdFieldsCache.get(domainType));
        }
        return new ArrayList<FieldMetadata>();
    }

    public MethodMetadata getIdentifierAccessor(final JavaType domainType) {
        updateCache(domainType);
        return domainTypeIdAccessorCache.get(domainType);
    }

    public List<FieldMetadata> getIdentifierFields(final JavaType domainType) {
        updateCache(domainType);
        if (domainTypeIdFieldsCache.containsKey(domainType)) {
            return new ArrayList<FieldMetadata>(
                    domainTypeIdFieldsCache.get(domainType));
        }
        else if (domainTypeEmbeddedIdFieldsCache.containsKey(domainType)) {
            return new ArrayList<FieldMetadata>(
                    domainTypeEmbeddedIdFieldsCache.get(domainType));
        }

        return new ArrayList<FieldMetadata>();
    }

    public JavaType getIdentifierType(final JavaType domainType) {
        updateCache(domainType);
        if (domainTypeIdCache.containsKey(domainType)) {
            return domainTypeIdCache.get(domainType);
        }
        return null;
    }

    private MemberDetails getMemberDetails(
            final ClassOrInterfaceTypeDetails typeDetails) {
        return memberDetailsScanner.getMemberDetails(getClass().getName(),
                typeDetails);
    }

    private MemberDetails getMemberDetails(final JavaType type) {
        final ClassOrInterfaceTypeDetails typeDetails = typeLocationService
                .getTypeDetails(type);
        if (typeDetails == null) {
            return null;
        }
        return memberDetailsScanner.getMemberDetails(getClass().getName(),
                typeDetails);
    }

    public MethodMetadata getVersionAccessor(final JavaType domainType) {
        updateCache(domainType);
        return domainTypeVersionAccessorCache.get(domainType);
    }

    public FieldMetadata getVersionField(final JavaType domainType) {
        updateCache(domainType);
        return domainTypeVersionFieldCache.get(domainType);
    }

    private boolean haveAssociatedTypesChanged(final JavaType javaType) {
        return typeLocationService.hasTypeChanged(getClass().getName(),
                javaType)
                || itdDiscoveryService.haveItdsChanged(getClass().getName(),
                        javaType);
    }

    private void populateEmbeddedIdFields(final MemberDetails details,
            final JavaType type) {
        final List<FieldMetadata> embeddedIdFields = MemberFindingUtils
                .getFieldsWithTag(details, CustomDataKeys.EMBEDDED_ID_FIELD);
        if (!embeddedIdFields.isEmpty()) {
            domainTypeEmbeddedIdFieldsCache.remove(type);
            domainTypeEmbeddedIdFieldsCache.put(type,
                    new ArrayList<FieldMetadata>());
            final MemberDetails memberDetails = getMemberDetails(embeddedIdFields
                    .get(0).getFieldType());
            if (memberDetails != null) {
                for (final FieldMetadata field : memberDetails.getFields()) {
                    if (!field.getCustomData().keySet()
                            .contains(CustomDataKeys.SERIAL_VERSION_UUID_FIELD)) {
                        domainTypeEmbeddedIdFieldsCache.get(type).add(field);
                    }
                }
            }
        }
        else if (domainTypeEmbeddedIdFieldsCache.containsKey(type)) {
            domainTypeEmbeddedIdFieldsCache.remove(type);
        }
    }

    private void populateIdAccessors(final MemberDetails details,
            final JavaType type) {
        final MethodMetadata idAccessor = MemberFindingUtils
                .getMostConcreteMethodWithTag(details,
                        CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
        if (idAccessor != null) {
            domainTypeIdAccessorCache.put(type, idAccessor);
        }
        else if (domainTypeIdAccessorCache.containsKey(type)) {
            domainTypeIdAccessorCache.remove(type);
        }
    }

    private void populateIdFields(final MemberDetails details,
            final JavaType type) {
        final List<FieldMetadata> idFields = MemberFindingUtils
                .getFieldsWithTag(details, CustomDataKeys.IDENTIFIER_FIELD);
        final List<FieldMetadata> embeddedIdFields = MemberFindingUtils
                .getFieldsWithTag(details, CustomDataKeys.EMBEDDED_ID_FIELD);
        if (!idFields.isEmpty()) {
            domainTypeIdFieldsCache.put(type, idFields);
        }
        else if (!embeddedIdFields.isEmpty()) {
            domainTypeIdFieldsCache.put(type, embeddedIdFields);
        }
        else if (domainTypeIdFieldsCache.containsKey(type)) {
            domainTypeIdFieldsCache.remove(type);
        }
    }

    private void populateIdTypes(final MemberDetails details,
            final JavaType type) {
        final List<FieldMetadata> idFields = MemberFindingUtils
                .getFieldsWithTag(details, CustomDataKeys.IDENTIFIER_FIELD);
        final List<FieldMetadata> embeddedIdFields = MemberFindingUtils
                .getFieldsWithTag(details, CustomDataKeys.EMBEDDED_ID_FIELD);
        if (!idFields.isEmpty()) {
            domainTypeIdCache.put(type, idFields.get(0).getFieldType());
        }
        else if (!embeddedIdFields.isEmpty()) {
            domainTypeIdCache.put(type, embeddedIdFields.get(0).getFieldType());
        }
        else {
            domainTypeIdCache.remove(type);
        }
    }

    private void populateVersionAccessor(final MemberDetails details,
            final JavaType type) {
        final MethodMetadata versionAccessor = MemberFindingUtils
                .getMostConcreteMethodWithTag(details,
                        CustomDataKeys.VERSION_ACCESSOR_METHOD);
        if (versionAccessor != null) {
            domainTypeVersionAccessorCache.put(type, versionAccessor);
        }
        else if (domainTypeVersionAccessorCache.containsKey(type)) {
            domainTypeVersionAccessorCache.remove(type);
        }
    }

    private void populateVersionField(final MemberDetails details,
            final JavaType type) {
        final List<FieldMetadata> versionFields = MemberFindingUtils
                .getFieldsWithTag(details, CustomDataKeys.VERSION_FIELD);
        if (!versionFields.isEmpty()) {
            domainTypeVersionFieldCache.put(type, versionFields.get(0));
        }
        else if (domainTypeVersionFieldCache.containsKey(type)) {
            domainTypeVersionFieldCache.remove(type);
        }
    }

    private void updateCache(final JavaType domainType) {
        if (!haveAssociatedTypesChanged(domainType)) {
            return;
        }

        final ClassOrInterfaceTypeDetails domainTypeDetails = typeLocationService
                .getTypeDetails(domainType);
        if (domainTypeDetails == null
                || !domainTypeDetails.getCustomData().keySet()
                        .contains(CustomDataKeys.PERSISTENT_TYPE)) {
            return;
        }

        final MemberDetails memberDetails = getMemberDetails(domainTypeDetails);

        // Update normal persistence ID fields cache
        populateIdTypes(memberDetails, domainType);

        // Update normal persistence ID cache
        populateIdFields(memberDetails, domainType);

        // Update embedded ID fields cache
        populateEmbeddedIdFields(memberDetails, domainType);

        // Update ID accessor cache
        populateIdAccessors(memberDetails, domainType);

        // Update version field cache
        populateVersionField(memberDetails, domainType);

        // Update version accessor cache
        populateVersionAccessor(memberDetails, domainType);
    }
}
