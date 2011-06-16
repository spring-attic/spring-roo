package org.springframework.roo.addon.layers.service;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class ServiceClassMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final JavaType SERVICE_ANNOTATION = new JavaType("org.springframework.stereotype.Service");
	private static final JavaType TRANSACTIONAL_ANNOTATION = new JavaType("org.springframework.transaction.annotation.Transactional");
	private static final String PROVIDES_TYPE_STRING = ServiceClassMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	// Fields
	private final MemberDetails governorDetails;
	private final ServiceAnnotationValues annotationValues;
	
	/**
	 * Constructor
	 *
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 * @param governorDetails (required)
	 * @param annotationValues (required)
	 * @param allCrudAdditions (required)
	 */
	public ServiceClassMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MemberDetails governorDetails, ServiceAnnotationValues annotationValues, Map<JavaType, Map<String, MemberTypeAdditions>> allCrudAdditions) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(allCrudAdditions, "CRUD additions required");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(governorDetails, "Governor member details required");
		
		this.annotationValues = annotationValues;
		this.governorDetails = governorDetails;
		
		for (final JavaType domainType : annotationValues.getDomainTypes()) {
			Map<String, MemberTypeAdditions> crudAdditions = allCrudAdditions.get(domainType);
			
			final MemberTypeAdditions findAllAdditions = crudAdditions.get(PersistenceCustomDataKeys.FIND_ALL_METHOD.name());
			builder.addMethod(getFindAllMethod(domainType, findAllAdditions));
			if (findAllAdditions != null) {
				findAllAdditions.copyClassOrInterfaceTypeDetailsIntoTargetTypeBuilder(findAllAdditions.getClassOrInterfaceTypeDetailsBuilder(), builder);
			}
		}
		
		// Introduce the @Service annotation via the ITD if it's not already on the service's Java class
		final AnnotationMetadata serviceAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorDetails, SERVICE_ANNOTATION);
		if (serviceAnnotation == null) {
			builder.addAnnotation(new AnnotationMetadataBuilder(SERVICE_ANNOTATION));
		}
		
		// Introduce the @Transactional annotation via the ITD if it's not already on the service's Java class
		if (annotationValues.isTransactional()) {
			final AnnotationMetadata transactionalAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorDetails, TRANSACTIONAL_ANNOTATION);
			if (transactionalAnnotation == null) {
				builder.addAnnotation(new AnnotationMetadataBuilder(TRANSACTIONAL_ANNOTATION));
			}
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	private MethodMetadata getFindAllMethod(JavaType domainType, MemberTypeAdditions findAllAdditions) {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getFindAllMethod());
		if (MemberFindingUtils.getMethod(governorDetails, methodName, null) != null) {
			// The governor already declares this method
			return null;
		}
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		// No further layer found, so let's create a simple dummy implementation
		if (findAllAdditions == null) {
			bodyBuilder.appendFormalLine("throw new IllegalStateException(\"Implement me!\");");
		} else {
			bodyBuilder.appendFormalLine("return " + findAllAdditions.getMethodSignature() + ";");
		}
		
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(domainType)), bodyBuilder).build();
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
