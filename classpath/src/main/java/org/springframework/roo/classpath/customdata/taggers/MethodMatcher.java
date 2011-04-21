package org.springframework.roo.classpath.customdata.taggers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * {@link MethodMetadata} specific implementation of {@link Matcher}. Matches
 * are based on field name which is dynamically determined based on: the {@link FieldMatcher}s
 * presented; the type of method (accessor/mutator); the default method name; the user specified
 * method name obtained from a particular Roo annotation; a plural/singular suffix of the
 * referenced entity; and, an additional suffix.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MethodMatcher implements Matcher<MethodMetadata> {
	private List<FieldMatcher> fieldTaggers = new ArrayList<FieldMatcher>();
	private boolean isAccessor = false;
	private CustomDataKey<MethodMetadata> customDataKey;
	private JavaType catalystAnnotationType;
	private JavaSymbolName userDefinedNameAttribute;
	private String defaultName;
	private boolean suffixPlural = false;
	private boolean suffixSingular = false;
	private String additionalSuffix = "";

	public MethodMatcher(List<FieldMatcher> fieldTaggers, CustomDataKey<MethodMetadata> customDataKey, boolean isAccessor) {
		this.fieldTaggers = fieldTaggers;
		this.customDataKey = customDataKey;
		this.isAccessor = isAccessor;
	}

	public MethodMatcher(CustomDataKey<MethodMetadata> customDataKey, JavaType catalystAnnotationType, JavaSymbolName userDefinedNameAttribute, String defaultName) {
		this.customDataKey = customDataKey;
		this.catalystAnnotationType = catalystAnnotationType;
		this.userDefinedNameAttribute = userDefinedNameAttribute;
		this.defaultName = defaultName;
	}

	public MethodMatcher(CustomDataKey<MethodMetadata> customDataKey, JavaType catalystAnnotationType, JavaSymbolName userDefinedNameAttribute, String defaultName, boolean suffixPlural, boolean suffixSingular) {
		this(customDataKey, catalystAnnotationType, userDefinedNameAttribute, defaultName);
		this.suffixPlural = suffixPlural;
		this.suffixSingular = suffixSingular;
	}

	public MethodMatcher(CustomDataKey<MethodMetadata> customDataKey, JavaType catalystAnnotationType, JavaSymbolName userDefinedNameAttribute, String defaultName, boolean suffixPlural, boolean suffixSingular, String additionalSuffix) {
		this(customDataKey, catalystAnnotationType, userDefinedNameAttribute, defaultName, suffixPlural, suffixSingular);
		this.additionalSuffix = additionalSuffix;
	}

	public List<MethodMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		return null; // TODO: This needs to be dealt with -JT
	}

	public CustomDataKey<MethodMetadata> getCustomDataKey() {
		return customDataKey;
	}

	public Object getTagValue(MethodMetadata key) {
		return null;
	}

	public List<MethodMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, HashMap<String, String> pluralMap) {
		List<FieldMetadata> fields = getFieldsInterestedIn(memberHoldingTypeDetailsList);
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		Set<JavaSymbolName> fieldNames = new HashSet<JavaSymbolName>();
		JavaSymbolName userDefinedMethodName = getUserDefinedMethod(memberHoldingTypeDetailsList, pluralMap);
		if (userDefinedMethodName == null) {
			for (FieldMetadata fieldMetadata : fields) {
				fieldNames.add(new JavaSymbolName(getPrefix() + StringUtils.capitalize(fieldMetadata.getFieldName().getSymbolName())));
			}
		} else {
			fieldNames.add(new JavaSymbolName(userDefinedMethodName.getSymbolName() + additionalSuffix));
		}
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			for (MethodMetadata methodMetadata : memberHoldingTypeDetails.getDeclaredMethods()) {
				if (fieldNames.contains(methodMetadata.getMethodName())) {
					methods.add(methodMetadata);
				}
			}
		}
		return methods;
	}

	private JavaSymbolName getUserDefinedMethod(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, HashMap<String, String> pluralMap) {
		if (catalystAnnotationType == null || userDefinedNameAttribute == null) {
			return null;
		}
		String suffix = suffixPlural || suffixSingular ? getSuffix(memberHoldingTypeDetailsList, suffixSingular, pluralMap) : "";
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getMostConcreteClassOrInterfaceTypeDetails(memberHoldingTypeDetailsList);
		for (AnnotationMetadata annotationMetadata : classOrInterfaceTypeDetails.getAnnotations()) {
			if (annotationMetadata.getAnnotationType().equals(catalystAnnotationType)) {
				AnnotationAttributeValue<?> annotationAttributeValue = annotationMetadata.getAttribute(userDefinedNameAttribute);
				if (annotationAttributeValue != null) {
					return new JavaSymbolName(annotationAttributeValue.getValue().toString() + suffix);
				}
				break;
			}
		}
		return defaultName == null ? null : new JavaSymbolName(defaultName + suffix);
	}

	private ClassOrInterfaceTypeDetails getMostConcreteClassOrInterfaceTypeDetails(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = null;
		// The last ClassOrInterfaceTypeDetails is the most concrete as dictated by the logic in MemberDetailsScannerImpl
		for (MemberHoldingTypeDetails aMemberHoldingTypeDetailsList : memberHoldingTypeDetailsList) {
			if (aMemberHoldingTypeDetailsList instanceof ClassOrInterfaceTypeDetails) {
				classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) aMemberHoldingTypeDetailsList;
			}
		}
		Assert.notNull(classOrInterfaceTypeDetails, "No concrete type found; cannot continue");
		return classOrInterfaceTypeDetails;

	}

	private String getSuffix(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, boolean singular, HashMap<String, String> pluralMap) {
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getMostConcreteClassOrInterfaceTypeDetails(memberHoldingTypeDetailsList);
		if (singular) {
			return classOrInterfaceTypeDetails.getName().getSimpleTypeName();
		}
		for (AnnotationMetadata annotationMetadata : classOrInterfaceTypeDetails.getAnnotations()) {
			if (annotationMetadata.getAnnotationType().equals(new JavaType("org.springframework.roo.addon.plural.RooPlural"))) {
				AnnotationAttributeValue<?> annotationAttributeValue = annotationMetadata.getAttribute(new JavaSymbolName("value"));
				if (annotationAttributeValue != null) {
					return annotationAttributeValue.getValue().toString();
				}
				break;
			}
		}
		return pluralMap.get(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
	}

	private List<FieldMetadata> getFieldsInterestedIn(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		for (FieldMatcher fieldTagger : fieldTaggers) {
			fields.addAll(fieldTagger.matches(memberHoldingTypeDetailsList));
		}
		return fields;
	}

	private String getPrefix() {
		return isAccessor ? "get" : "set";
	}
}
