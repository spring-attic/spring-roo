package org.springframework.roo.addon.equals;

import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.OBJECT;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooEquals}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class EqualsMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = EqualsMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType EQUALS_BUILDER = new JavaType("org.apache.commons.lang.builder.EqualsBuilder");
	private static final JavaType HASH_CODE_BUILDER = new JavaType("org.apache.commons.lang.builder.HashCodeBuilder");
	private static final String OBJECT_NAME = "obj";

	// Fields
	private final EqualsAnnotationValues annotationValues;
	private final List<FieldMetadata> locatedFields;

	/** 
	 * Constructor
	 * 
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param locatedFields
	 */
	public EqualsMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final EqualsAnnotationValues annotationValues, final List<FieldMetadata> locatedFields) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(locatedFields, "Located fields required");

		this.annotationValues = annotationValues;
		this.locatedFields = locatedFields;
	
		// Generate the display name method
		builder.addMethod(getEqualsMethod());
		builder.addMethod(getHashCodeMethod());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Obtains the display name method for this type, if available.
	 * <p>
	 * If the user provided a non-default name for "getEquals", that method will be returned.
	 *
	 * @return the display name method declared on this type or that will be introduced (or null if undeclared and not introduced)
	 */
	private MethodMetadata getEqualsMethod() {
		JavaSymbolName methodName = new JavaSymbolName("equals");
		JavaType parameterType = OBJECT;
		if (getGovernorMethod(methodName, parameterType) != null) {
			return null;
		}

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(OBJECT_NAME));

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(EQUALS_BUILDER);

		String typeName = destination.getSimpleTypeName();

		// Create the method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + OBJECT_NAME + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return false;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("if (this == " + OBJECT_NAME + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return true;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("if (!(" + OBJECT_NAME + " instanceof " + typeName + ")) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return false;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(typeName + " rhs = (" + typeName + ") " + OBJECT_NAME + ";");

		StringBuilder builder = new StringBuilder();
		builder.append("return new EqualsBuilder()");
		if (annotationValues.isAppendSuper()) {
			builder.append(".appendSuper(super.equals(" + OBJECT_NAME + "))");
		}
		for (FieldMetadata field : locatedFields) {
			builder.append(".append(" + field.getFieldName() + ", rhs." + field.getFieldName() +")");
		}
		builder.append(".isEquals();");
		
		bodyBuilder.appendFormalLine(builder.toString());

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, BOOLEAN_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getHashCodeMethod() {
		JavaSymbolName methodName = new JavaSymbolName("hashCode");
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HASH_CODE_BUILDER);

		// Create the method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		StringBuilder builder = new StringBuilder();
		builder.append("return new HashCodeBuilder()");
		if (annotationValues.isAppendSuper()) {
			builder.append(".appendSuper(super.hashCode())");
		}
		for (FieldMetadata field : locatedFields) {
			builder.append(".append(" + field.getFieldName() +")");
		}
		builder.append(".toHashCode();");

		bodyBuilder.appendFormalLine(builder.toString());

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, INT_PRIMITIVE, bodyBuilder);
		return methodBuilder.build();
	}

	@Override
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

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
