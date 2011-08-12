package org.springframework.roo.addon.layers.service;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * The metadata about a service interface within a user project
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ServiceInterfaceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final int PUBLIC_ABSTRACT = Modifier.PUBLIC | Modifier.ABSTRACT;
	private static final String PROVIDES_TYPE_STRING = ServiceInterfaceMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final InvocableMemberBodyBuilder BODY = new InvocableMemberBodyBuilder();
	
	// Fields
	private final MemberDetails governorDetails;
	private final ServiceAnnotationValues annotationValues;
	
	/**
	 * Constructor
	 *
	 * @param identifier (required)
	 * @param aspectName (required)
	 * @param governorPhysicalTypeMetadata (required)
	 * @param governorDetails (required)
	 * @param domainTypeToIdTypeMap (required)
	 * @param annotationValues (required)
	 * @param domainTypePlurals 
	 */
	public ServiceInterfaceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MemberDetails governorDetails, Map<JavaType, JavaType> domainTypeToIdTypeMap, ServiceAnnotationValues annotationValues, Map<JavaType, String> domainTypePlurals) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(governorDetails, "Governor member details required");
		Assert.notNull(domainTypeToIdTypeMap, "Domain type to ID type map required required");
		Assert.notNull(domainTypePlurals, "Domain type plural values required");
		
		this.annotationValues = annotationValues;
		this.governorDetails = governorDetails;
		
		for (final JavaType domainType : domainTypeToIdTypeMap.keySet()) {
			final String plural = domainTypePlurals.get(domainType);
			for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
				builder.addMethod(getMethod(method, domainType, domainTypeToIdTypeMap.get(domainType), plural));
			}
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Returns the metadata for declaring the given method in the service interface
	 * 
	 * @param method the method to declare
	 * @param domainType the domain type being managed
	 * @param plural the domain type's plural
	 * @return <code>null</code> if the method isn't required or is already
	 * declared in the governor
	 */
	private MethodMetadata getMethod(final ServiceLayerMethod method, final JavaType domainType, final JavaType idType, final String plural) {
		final JavaSymbolName methodName = method.getSymbolName(annotationValues, domainType, plural);
		if (methodName == null || MemberFindingUtils.getMethod(governorDetails, methodName, null) != null) {
			// We don't want this method, or the governor already declares it
			return null;
		}
		return new MethodMetadataBuilder(
				getId(),
				PUBLIC_ABSTRACT,
				methodName,
				method.getReturnType(domainType),
				AnnotatedJavaType.convertFromJavaTypes(method.getParameterTypes(domainType, idType)),
				method.getParameterNames(domainType, idType),
				BODY)
		.build();
	}

	public ServiceAnnotationValues getServiceAnnotationValues() {
		return annotationValues;
	}
	
	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}
}
