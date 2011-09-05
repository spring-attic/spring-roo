package org.springframework.roo.addon.layers.service;

import static org.springframework.roo.model.SpringJavaType.SERVICE;
import static org.springframework.roo.model.SpringJavaType.TRANSACTIONAL;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class ServiceClassMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = ServiceClassMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	/**
	 * Constructor
	 *
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 * @param governorDetails (required)
	 * @param annotationValues (required)
	 * @param domainTypeToIdTypeMap (required)
	 * @param allCrudAdditions any additions to be made to the service class in
	 * order to invoke lower-layer methods (required)
	 * @param domainTypePlurals the plurals of each domain type managed by the service
	 */
	public ServiceClassMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MemberDetails governorDetails, ServiceAnnotationValues annotationValues, Map<JavaType, JavaType> domainTypeToIdTypeMap, Map<JavaType, Map<ServiceLayerMethod, MemberTypeAdditions>> allCrudAdditions, Map<JavaType, String> domainTypePlurals) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(allCrudAdditions, "CRUD additions required");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(governorDetails, "Governor details required");
		Assert.notNull(domainTypePlurals, "Domain type plurals required");
		
		for (final JavaType domainType : domainTypeToIdTypeMap.keySet()) {
			JavaType idType = domainTypeToIdTypeMap.get(domainType);
			final Map<ServiceLayerMethod, MemberTypeAdditions> crudAdditions = allCrudAdditions.get(domainType);
			for (final ServiceLayerMethod method : ServiceLayerMethod.values()) {
				final JavaSymbolName methodName = method.getSymbolName(annotationValues, domainType, domainTypePlurals.get(domainType));
				if (methodName != null && MemberFindingUtils.isMethodDeclaredBy(governorDetails, methodName, method.getParameterTypes(domainType, idType), getId())) {
					// The method is desired and the service class' Java file doesn't contain it, so generate it
					final MemberTypeAdditions lowerLayerCallAdditions = crudAdditions.get(method);
					if (lowerLayerCallAdditions != null) {
						// A lower layer implements it
						lowerLayerCallAdditions.copyAdditionsTo(builder, governorTypeDetails);
					}
					final String body = method.getBody(lowerLayerCallAdditions);
					final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
					bodyBuilder.appendFormalLine(body);
					builder.addMethod(
						new MethodMetadataBuilder(
							getId(),
							Modifier.PUBLIC,
							methodName,
							method.getReturnType(domainType),
							AnnotatedJavaType.convertFromJavaTypes(method.getParameterTypes(domainType, idType)),
							method.getParameterNames(domainType, idType),
							bodyBuilder
						)
					);
				}
			}
		}
		
		// Introduce the @Service annotation via the ITD if it's not already on the service's Java class
		final AnnotationMetadata serviceAnnotation = new AnnotationMetadataBuilder(SERVICE).build();
		if (MemberFindingUtils.isRequestingAnnotatedWith(governorDetails, serviceAnnotation, getId())) {
			builder.addAnnotation(serviceAnnotation);
		}
		
		// Introduce the @Transactional annotation via the ITD if it's not already on the service's Java class
		if (annotationValues.isTransactional()) {
			final AnnotationMetadata transactionalAnnotation = new AnnotationMetadataBuilder(TRANSACTIONAL).build();
			if (MemberFindingUtils.isRequestingAnnotatedWith(governorDetails, serviceAnnotation, getId())) {
				builder.addAnnotation(transactionalAnnotation);
			}
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
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
