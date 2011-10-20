package org.springframework.roo.addon.displaystring;

import static org.springframework.roo.model.JavaType.STRING;
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
import org.springframework.roo.project.ContextualPath;
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
	private static final int MAX_LIST_VIEW_FIELDS = 4;
	private static final String DEFAULT_SEPARATOR = " ";
	private static final String PROVIDES_TYPE_STRING = DisplayStringMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	/**
	 * A method call and its associated imports
	 *
	 * @author Andrew Swan
	 * @since 1.2.0
	 */
	private static class MethodCall {

		// Fields
		final String codeSnippet;
		final JavaType[] imports;

		/**
		 * Constructor
		 *
		 * @param codeSnippet the Java snippet that calls the method
		 * @param imports any imports required for the snippet to compile
		 */
		private MethodCall(final String codeSnippet, final JavaType... imports) {
			Assert.hasText(codeSnippet, "Method call is required");
			this.imports = imports;
			this.codeSnippet = codeSnippet;
		}

		private String getCodeSnippet() {
			return codeSnippet;
		}

		private JavaType[] getImports() {
			return imports;
		}
	}

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
		final JavaSymbolName methodName = new JavaSymbolName("getDisplayString");
		if (governorHasMethod(methodName)) {
			return null;
		}
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getDisplayMethodBody(getDisplayMethodCalls(), annotationValues.getSeparator()));
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, STRING, bodyBuilder).build();
	}
	
	/**
	 * Generates the body of the <code>getDisplayString</code> method
	 * 
	 * @param displayMethods the method calls that return the values to be
	 * displayed (required, can be empty)
	 * @param separator the separator to show between each display value
	 * (defaulted to {@value #DEFAULT_SEPARATOR} if blank)
	 * @return a non-empty method body
	 */
	private String getDisplayMethodBody(final List<String> displayMethods, final String separator) {
		final StringBuilder builder = new StringBuilder("return new StringBuilder()");
		final String delimiter = ".append(\"" + StringUtils.defaultIfEmpty(separator, DEFAULT_SEPARATOR) + "\")";
		builder.append(StringUtils.collectionToDelimitedString(displayMethods, delimiter, ".append(", ")"));
		builder.append(".toString();");
		return builder.toString();
	}

	/**
	 * Returns the invocations of the display methods, e.g. "getFirstName()"
	 *
	 * @return a non-<code>null</code> list (can be empty)
	 */
	private List<String> getDisplayMethodCalls() {
		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();

		final List<?> displayFields = CollectionUtils.arrayToList(annotationValues.getFields());
		final List<String> displayMethodCalls = new ArrayList<String>();
		for (final MethodMetadata accessor : locatedAccessors) {
			final MethodCall accessorCall = getAccessorCall(accessor);
			if (accessorCall != null && isDisplayFieldAccessor(displayFields, displayMethodCalls, accessor)) {
				displayMethodCalls.add(accessorCall.getCodeSnippet());
				imports.addImports(accessorCall.getImports());
			}
		}

		if (displayMethodCalls.isEmpty() && identifierAccessor != null) {
			// Fall back to displaying the entity's ID
			displayMethodCalls.add(identifierAccessor.getMethodName().getSymbolName() + "()");
		}
		return displayMethodCalls;
	}

	/**
	 * Returns a Java snippet that invokes the given accessor for display purposes.
	 *
	 * @param accessor the accessor to check (required)
	 * @return a String like "getFirstName()"; <code>null</code> if this
	 * accessor is not suitable for a display value
	 */
	private MethodCall getAccessorCall(final MethodMetadata accessor) {
		if (accessor.getReturnType().isMultiValued()) {
			return null;
		}
		final String accessorName = accessor.getMethodName().getSymbolName();
		if (CALENDAR.equals(accessor.getReturnType())) {
			final String codeSnippet = accessorName + "() == null ? \"\" : DateFormat.getDateInstance(DateFormat.LONG).format(" + accessorName + "().getTime())";
			return new MethodCall(codeSnippet, DATE_FORMAT);
		}
		if (DATE.equals(accessor.getReturnType())) {
			final String codeSnippet = accessorName + "() == null ? \"\" : DateFormat.getDateInstance(DateFormat.LONG).format(" + accessorName + "())";
			return new MethodCall(codeSnippet, DATE_FORMAT);
		}
		return new MethodCall(accessorName + "()");
	}

	/**
	 * Indicates whether the given accessor is for a field to be displayed
	 *
	 * @param displayFields the fields specified by the user (can be empty)
	 * @param displayMethods
	 * @param accessor
	 * @return
	 */
	private boolean isDisplayFieldAccessor(final List<?> displayFields, final List<String> displayMethods, final MethodMetadata accessor) {
		if (CollectionUtils.isEmpty(displayFields)) {
			// The user specified no display fields; use the first "x" non-ID accessors
			return displayMethods.size() <= MAX_LIST_VIEW_FIELDS && !isIdentifierAccessor(accessor);
		}
		// The user did specify some display fields; see if this was one of them
		final String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
		return displayFields.contains(StringUtils.uncapitalize(fieldName));
	}

	private boolean isIdentifierAccessor(final MethodMetadata accessor) {
		return accessor.hasSameName(identifierAccessor);
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
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
