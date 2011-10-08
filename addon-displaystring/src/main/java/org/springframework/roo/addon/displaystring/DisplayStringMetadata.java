package org.springframework.roo.addon.displaystring;

import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JdkJavaType.ARRAYS;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.DATE_FORMAT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDisplayString}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class DisplayStringMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = DisplayStringMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final int MAX_LIST_VIEW_FIELDS = 4;

	// Fields
	private final DisplayStringAnnotationValues annotationValues;
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
	public DisplayStringMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final DisplayStringAnnotationValues annotationValues, final List<MethodMetadata> locatedAccessors, final MethodMetadata identifierAccessor) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(locatedAccessors, "Located accessors required");

		this.annotationValues = annotationValues;
		this.locatedAccessors = locatedAccessors;
		this.identifierAccessor = identifierAccessor;

		// Generate the getDisplayString method
		builder.addMethod(getDisplayStringMethod());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Obtains the display string method for this type, if available.
	 * <p>
	 * If the user provided a non-default name for "getDisplayString", that method will be returned.
	 *
	 * @return the display name method declared on this type or that will be introduced (or null if undeclared and not introduced)
	 */
	private MethodMetadata getDisplayStringMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getDisplayString");
		if (getGovernorMethod(methodName) != null) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		final List<?> fieldsList = CollectionUtils.arrayToList(annotationValues.getFields());
		int methodCount = 0;
		final List<String> displayMethods = new ArrayList<String>();
		for (MethodMetadata accessor : locatedAccessors) {
			String accessorName = accessor.getMethodName().getSymbolName();
			String accessorText;
			if (accessor.getReturnType().isCommonCollectionType()) {
				continue;
			} else if (accessor.getReturnType().isArray()) {
				imports.addImport(ARRAYS);
				accessorText = "Arrays.toString(" + accessorName + "())";
			} else if (CALENDAR.equals(accessor.getReturnType())) {
				imports.addImport(DATE_FORMAT);
				accessorText = accessorName + "() == null ? \"\" : DateFormat.getDateInstance(DateFormat.LONG).format(" + accessorName + "().getTime())";
			} else if (DATE.equals(accessor.getReturnType())) {
				imports.addImport(DATE_FORMAT);
				accessorText = accessorName + "() == null ? \"\" : DateFormat.getDateInstance(DateFormat.LONG).format(" + accessorName + "())";
			} else {
				accessorText = accessorName + "()";
			}

			if (!fieldsList.isEmpty()) {
				String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
				if (fieldsList.contains(StringUtils.uncapitalize(fieldName))) {
					displayMethods.add(accessorText);
				}
				continue;
			}

			if (methodCount <= MAX_LIST_VIEW_FIELDS) {
				if (identifierAccessor != null && accessor.hasSameName(identifierAccessor)) {
					continue;
				}
				methodCount++;
				displayMethods.add(accessorText);
			}
		}

		if (displayMethods.isEmpty()) {
			return null;
		}

		String separator = StringUtils.defaultIfEmpty(annotationValues.getSeparator(), " ");
		final StringBuilder builder = new StringBuilder("return new StringBuilder()");
		for (int i = 0; i < displayMethods.size(); i++) {
			if (i > 0) {
				builder.append(".append(\"").append(separator).append("\")");
			}
			builder.append(".append(").append(displayMethods.get(i)).append(")");
		}
		builder.append(".toString();");

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(builder.toString());

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
