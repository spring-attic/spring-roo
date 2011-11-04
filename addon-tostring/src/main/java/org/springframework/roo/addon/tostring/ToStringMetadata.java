package org.springframework.roo.addon.tostring;

import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAYS;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

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
import org.springframework.roo.project.ContextualPath;
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
	private final MethodMetadata identifierAccessor;

	/**
	 * Constructor
	 *
	 * @param identifier
	 * @param aspectName
	 * @param governorPhysicalTypeMetadata
	 * @param annotationValues
	 * @param locatedAccessors
	 * @param identifierAccessor 
	 */
	public ToStringMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final ToStringAnnotationValues annotationValues, final List<MethodMetadata> locatedAccessors, final MethodMetadata identifierAccessor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(locatedAccessors, "Located accessors required");

		this.annotationValues = annotationValues;
		this.locatedAccessors = locatedAccessors;
		this.identifierAccessor = identifierAccessor;

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

		final List<MethodMetadata> toStringAccessors = getToStringAccessors();
		if (toStringAccessors.isEmpty()) {
			return null;
		}

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getToStringMethodBody(toStringAccessors));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, STRING, bodyBuilder);
		return methodBuilder.build();
	}

	private List<MethodMetadata> getToStringAccessors() {
		final List<?> excludeFieldsList = CollectionUtils.arrayToList(annotationValues.getExcludeFields());
		final List<MethodMetadata> toStringAccessors = new LinkedList<MethodMetadata>();
		for (final MethodMetadata accessor : locatedAccessors) {
			String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
			if (excludeFieldsList.contains(StringUtils.uncapitalize(fieldName))) {
				continue;
			}
			if (accessor.hasSameName(identifierAccessor)) {
				toStringAccessors.add(0, accessor);
			} else {
				toStringAccessors.add(accessor);
			}
		}
		return toStringAccessors;
	}

	private String getToStringMethodBody(final List<MethodMetadata> toStringAccessors) {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		int index = 0;
		final StringBuilder builder = new StringBuilder();
		for (final MethodMetadata accessor : toStringAccessors) {
			index++;

			String accessorName = accessor.getMethodName().getSymbolName();
			String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
			String accessorText = accessorName + "()";
			if (accessor.getReturnType().isCommonCollectionType()) {
				accessorText = accessorName + "() == null ? \"null\" : " + accessorName + "().size()";
			} else if (accessor.getReturnType().isArray()) {
				imports.addImport(ARRAYS);
				accessorText = "Arrays.toString(" + accessorName + "())";
			} else if (CALENDAR.equals(accessor.getReturnType())) {
				accessorText = accessorName + "() == null ? \"null\" : " + accessorName + "().getTime()";
			}

			builder.append("        ").append("sb.append(\"").append(fieldName).append(": \").append(").append(accessorText).append(")");
			builder.append(index < toStringAccessors.size() ? ".append(\", \");" : ";").append(StringUtils.LINE_SEPARATOR);
		}
		builder.insert(0, StringUtils.LINE_SEPARATOR).insert(0, "StringBuilder sb = new StringBuilder();").append("        return sb.toString();");

		return builder.toString();
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

	public static String createIdentifier(final JavaType javaType, final ContextualPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static ContextualPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
