package org.springframework.roo.addon.web.mvc.controller.details;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Service to retrieve various metadata information for use by Web scaffolding
 * add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public interface WebMetadataService {

    Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions(
            JavaType domainType, String metadataIdentificationString);

    Map<JavaSymbolName, DateTimeFormatDetails> getDatePatterns(
            JavaType javaType, MemberDetails memberDetails,
            String metadataIdentificationString);

    List<JavaTypeMetadataDetails> getDependentApplicationTypeMetadata(
            JavaType javaType, MemberDetails memberDetails,
            String metadataIdentificationString);

    /**
     * Returns details of the dynamic finders for the given form backing type
     * 
     * @param formBackingType
     * @param formBackingTypeDetails
     * @param consumingMetadataId the ID of the
     *            {@link org.springframework.roo.metadata.MetadataItem} that's
     *            using these details
     * @return <code>null</code> if this information is not currently available
     */
    Set<FinderMetadataDetails> getDynamicFinderMethodsAndFields(
            JavaType formBackingType, MemberDetails formBackingTypeDetails,
            String consumingMetadataId);

    FieldMetadata getIdentifierField(JavaType javaType);

    JavaTypeMetadataDetails getJavaTypeMetadataDetails(JavaType javaType,
            MemberDetails memberDetails, String metadataIdentificationString);

    JavaTypePersistenceMetadataDetails getJavaTypePersistenceMetadataDetails(
            JavaType javaType, MemberDetails memberDetails,
            String metadataIdentificationString);

    MemberDetails getMemberDetails(JavaType javaType);

    /**
     * Returns details of the Java types that are related to the given type
     * 
     * @param baseType the type for which to obtain related types
     * @param baseTypeDetails the details of the given type
     * @param metadataId the ID of the
     *            {@link org.springframework.roo.metadata.MetadataItem}
     *            consuming the returned details; required for registering the
     *            necessary metadata dependencies
     * @return a non-<code>null</code> map that includes the given type
     */
    SortedMap<JavaType, JavaTypeMetadataDetails> getRelatedApplicationTypeMetadata(
            JavaType baseType, MemberDetails baseTypeDetails, String metadataId);

    List<FieldMetadata> getScaffoldEligibleFieldMetadata(JavaType javaType,
            MemberDetails memberDetails, String metadataIdentificationString);

    /**
     * @deprecated use {@link TypeLocationService#isInProject(JavaType)} instead
     */
    @Deprecated
    boolean isApplicationType(JavaType javaType);

    boolean isRooIdentifier(JavaType javaType, MemberDetails memberDetails);
}
