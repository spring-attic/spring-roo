package org.springframework.roo.addon.tostring;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
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
	private static final String PROVIDES_TYPE_STRING = ToStringMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private List<MethodMetadata> publicAccessors;

	// From annotation
	@AutoPopulate private String toStringMethod = "toString";
	@AutoPopulate private String[] excludeFields;

	public ToStringMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, List<MethodMetadata> publicAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(publicAccessors, "Public accessors required");

		this.publicAccessors = publicAccessors;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooToString.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Generate the toString
		MethodMetadata toStringMethod = getToStringMethod();
		builder.addMethod(toStringMethod);

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
		// Compute the relevant toString method name
		JavaSymbolName methodName = new JavaSymbolName("toString");
		if (!this.toStringMethod.equals("")) {
			methodName = new JavaSymbolName(this.toStringMethod);
		}

		// See if the type itself declared the method
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}

		// Decide whether we need to produce the toString method
		if (!this.toStringMethod.equals("")) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("StringBuilder sb = new StringBuilder();");

			/** Key: field name, Value: accessor name */
			Map<String, String> map = new LinkedHashMap<String, String>();

			/** Field names */
			List<String> order = new ArrayList<String>();

			Set<String> excludeFieldsSet = new LinkedHashSet<String>();
			if (excludeFields != null && excludeFields.length > 0) {
				Collections.addAll(excludeFieldsSet, excludeFields);
			}

			for (MethodMetadata accessor : publicAccessors) {
				String accessorName = accessor.getMethodName().getSymbolName();
				String fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor).getSymbolName();
				if (!excludeFieldsSet.contains(StringUtils.uncapitalize(fieldName)) && !map.containsKey(fieldName)) {
					String accessorText = accessorName + "()";
					if (accessor.getReturnType().isCommonCollectionType()) {
						accessorText = accessorName + "() == null ? \"null\" : " + accessorName + "().size()";
					} else if (accessor.getReturnType().isArray()) {
						accessorText = "java.util.Arrays.toString(" + accessorName + "())";
					} else if (Calendar.class.getName().equals(accessor.getReturnType().getFullyQualifiedTypeName())) {
						accessorText = accessorName + "() == null ? \"null\" : " + accessorName + "().getTime()";
					}
					map.put(fieldName, accessorText);
					order.add(fieldName);
				}
			}

			if (!order.isEmpty()) {
				int index = 0;
				int size = map.keySet().size();
				for (String fieldName : order) {
					index++;
					String accessorText = map.get(fieldName);
					StringBuilder string = new StringBuilder();
					string.append("sb.append(\"" + fieldName + ": \").append(" + accessorText + ")");
					if (index < size) {
						string.append(".append(\", \")");
					}
					string.append(";");
					bodyBuilder.appendFormalLine(string.toString());
				}

				bodyBuilder.appendFormalLine("return sb.toString();");

				MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType("java.lang.String"), bodyBuilder);
				result = methodBuilder.build();
			}
		}

		return result;
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
}
