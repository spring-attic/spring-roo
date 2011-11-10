package org.springframework.roo.addon.tostring;

import static org.springframework.roo.model.JavaType.STRING;

import java.lang.reflect.Modifier;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooToString}.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class ToStringMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = ToStringMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType TO_STRING_BUILDER = new JavaType("org.apache.commons.lang3.builder.ReflectionToStringBuilder");
	private static final JavaType TO_STRING_STYLE = new JavaType("org.apache.commons.lang3.builder.ToStringStyle");

	// Fields
	private final ToStringAnnotationValues annotationValues;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 */
	public ToStringMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final ToStringAnnotationValues annotationValues) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");

		this.annotationValues = annotationValues;

		// Generate the toString() method
		builder.addMethod(getToStringMethod());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Obtains the "toString" method for this type, if available.
	 * <p>
	 * If the user provided a non-default name for "toString", that method will be returned.
	 *
	 * @return the "toString" method declared on this type or that will be introduced (or null if undeclared and not introduced)
	 */
	public MethodMetadata getToStringMethod() {
		String toStringMethod = annotationValues.getToStringMethod();
		if (!StringUtils.hasText(toStringMethod)) {
			return null;
		}

		// Compute the relevant toString method name
		JavaSymbolName methodName = new JavaSymbolName(toStringMethod);

		// See if the type itself declared the method
		if (governorHasMethod(methodName)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(TO_STRING_BUILDER);
		imports.addImport(TO_STRING_STYLE);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String[] excludeFields = annotationValues.getExcludeFields();
		String str;
		if (excludeFields != null && excludeFields.length > 0) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < excludeFields.length; i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append("\"").append(excludeFields[i]).append("\"");
			}
			str = "new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).setExcludeFieldNames(" + builder.toString() + ").toString();";
		} else {
			str = "ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);";
		}

		final int length = annotationValues.getLength();
		if (length > 1) {
			bodyBuilder.appendFormalLine("String str = " + str);
			bodyBuilder.appendFormalLine("return str != null && str.length() > " + length + " ? str.substring(0, " + length + ") + \"...\" : str;");
		} else {
			bodyBuilder.appendFormalLine("return " + str);
		}

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, STRING, bodyBuilder).build();
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

	public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static LogicalPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
