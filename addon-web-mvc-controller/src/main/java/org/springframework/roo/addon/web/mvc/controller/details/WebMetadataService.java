package org.springframework.roo.addon.web.mvc.controller.details;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Service to retrieve various metadata information for use by Web scaffolding add-ons.
 *
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public interface WebMetadataService {

	SortedMap<JavaType, JavaTypeMetadataDetails> getRelatedApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	List<JavaTypeMetadataDetails> getDependentApplicationTypeMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	List<FieldMetadata> getScaffoldEligibleFieldMetadata(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	JavaTypePersistenceMetadataDetails getJavaTypePersistenceMetadataDetails(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	boolean isRooIdentifier(JavaType javaType, MemberDetails memberDetails);

	boolean isApplicationType(JavaType javaType);

	Map<JavaSymbolName, DateTimeFormatDetails> getDatePatterns(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	Set<FinderMetadataDetails> getDynamicFinderMethodsAndFields(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	JavaTypeMetadataDetails getJavaTypeMetadataDetails(JavaType javaType, MemberDetails memberDetails, String metadataIdentificationString);

	MemberDetails getMemberDetails(JavaType javaType);
}
