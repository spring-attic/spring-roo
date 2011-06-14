package org.springframework.roo.addon.layers.service;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class ServiceInterfaceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = ServiceInterfaceMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private ServiceAnnotationValues annotationValues;
	private MemberDetails governorDetails;
	
	public ServiceInterfaceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MemberDetails governorDetails, ServiceAnnotationValues annotationValues) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(governorDetails, "Governor member details required");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(governorDetails, "Governor member details required");
		
		this.annotationValues = annotationValues;
		this.governorDetails = governorDetails;
		
		for (JavaType domainType : annotationValues.getDomainTypes()) {
			builder.addMethod(getFindAllMethod(domainType));
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	private MethodMetadata getFindAllMethod(JavaType domainType) {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getFindAllMethod());
		// FIXME
//		if (MemberFindingUtils.getMethod(governorDetails, methodName, null) != null) {
//			return null;
//		}
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("ignore");
		
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.ABSTRACT, methodName, new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(domainType)), bodyBuilder).build();
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
