package org.springframework.roo.addon.layers.service;

import java.lang.reflect.Modifier;
import java.util.Arrays;
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
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.LayerUtils;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class ServiceInterfaceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
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
	 * @param annotationValues (required)
	 * @param domainTypePlurals 
	 */
	public ServiceInterfaceMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, MemberDetails governorDetails, ServiceAnnotationValues annotationValues, Map<JavaType, String> domainTypePlurals) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(governorDetails, "Governor member details required");
		Assert.notNull(domainTypePlurals, "Domain type plural values required");
		
		this.annotationValues = annotationValues;
		this.governorDetails = governorDetails;
		
		for (JavaType domainType : annotationValues.getDomainTypes()) {
			builder.addMethod(getFindAllMethod(domainType, domainTypePlurals.get(domainType)));
			builder.addMethod(getSaveMethod(domainType));
			builder.addMethod(getUpdateMethod(domainType));
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	private MethodMetadata getFindAllMethod(final JavaType domainType, final String plural) {
		final JavaSymbolName methodName = new JavaSymbolName(annotationValues.getFindAllMethod() + plural);
		if (MemberFindingUtils.getMethod(governorDetails, methodName, null) != null) {
			// The governor already declares this method
			return null;
		}
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.ABSTRACT, methodName, new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(domainType)), BODY).build();
	}
	
	private MethodMetadata getSaveMethod(final JavaType domainType) {
		final JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSaveMethod() + domainType.getSimpleTypeName());
		if (MemberFindingUtils.getMethod(governorDetails, methodName, null) != null || annotationValues.getSaveMethod().equals("")) {
			// The governor already declares this method
			return null;
		}
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.ABSTRACT, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(Arrays.asList(domainType)), Arrays.asList(LayerUtils.getTypeName(domainType)), BODY).build();
	}
	
	private MethodMetadata getUpdateMethod(final JavaType domainType) {
		final JavaSymbolName methodName = new JavaSymbolName(annotationValues.getUpdateMethod() + domainType.getSimpleTypeName());
		if (MemberFindingUtils.getMethod(governorDetails, methodName, null) != null || annotationValues.getUpdateMethod().equals("")) {
			// The governor already declares this method
			return null;
		}
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.ABSTRACT, methodName, domainType, AnnotatedJavaType.convertFromJavaTypes(Arrays.asList(domainType)), Arrays.asList(LayerUtils.getTypeName(domainType)), BODY).build();
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
