package org.springframework.roo.addon.tostring;

import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAYS;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;
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

	// Fields
	private final ToStringAnnotationValues annotationValues;
	private final List<MethodMetadata> locatedAccessors;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param locatedAccessors
	 */
	public ToStringMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final ToStringAnnotationValues annotationValues, final List<MethodMetadata> locatedAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(locatedAccessors, "Located accessors required");

		this.annotationValues = annotationValues;
		this.locatedAccessors = locatedAccessors;

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
		if (getGovernorMethod(methodName) != null) {
			return null;
		}
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		final List<?> excludeFieldsList = CollectionUtils.arrayToList(annotationValues.getExcludeFields());
		final Map<String, String> map = new LinkedHashMap<String, String>();
		for (MethodMetadata accessor : locatedAccessors) {
			String accessorName = accessor.getMethodName().getSymbolName();
			String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
			if (excludeFieldsList.contains(StringUtils.uncapitalize(fieldName))) {
				continue;
			}

			String accessorText = accessorName + "()";
			if (accessor.getReturnType().isCommonCollectionType()) {
				accessorText = accessorName + "() == null ? \"null\" : " + accessorName + "().size()";
			} else if (accessor.getReturnType().isArray()) {
				imports.addImport(ARRAYS);
				accessorText = "Arrays.toString(" + accessorName + "())";
			} else if (CALENDAR.equals(accessor.getReturnType())) {
				accessorText = accessorName + "() == null ? \"null\" : " + accessorName + "().getTime()";
			}

			map.put(fieldName, accessorText);
		}

		if (map.isEmpty()) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("StringBuilder sb = new StringBuilder();");

		int index = 0;
		int size = map.size();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			index++;
			StringBuilder builder = new StringBuilder();
			builder.append("sb.append(\"").append(entry.getKey()).append(": \").append(").append(entry.getValue()).append(")");
			if (index < size) {
				builder.append(".append(\", \")");
			}
			builder.append(";");
			bodyBuilder.appendFormalLine(builder.toString());
		}
		bodyBuilder.appendFormalLine("return sb.toString();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, STRING, bodyBuilder);
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
