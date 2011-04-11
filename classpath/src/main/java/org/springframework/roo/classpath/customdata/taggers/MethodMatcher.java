package org.springframework.roo.classpath.customdata.taggers;

import org.jvnet.inflector.Noun;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
		this.customDataKey = customDataKey;
		this.catalystAnnotationType = catalystAnnotationType;
		this.userDefinedNameAttribute = userDefinedNameAttribute;
		this.defaultName = defaultName;
		this.suffixPlural = suffixPlural;
		this.suffixSingular = suffixSingular;
	}

	public MethodMatcher(CustomDataKey<MethodMetadata> customDataKey, JavaType catalystAnnotationType, JavaSymbolName userDefinedNameAttribute, String defaultName, boolean suffixPlural, boolean suffixSingular, String additionalSuffix) {
		this.customDataKey = customDataKey;
		this.catalystAnnotationType = catalystAnnotationType;
		this.userDefinedNameAttribute = userDefinedNameAttribute;
		this.defaultName = defaultName;
		this.suffixPlural = suffixPlural;
		this.suffixSingular = suffixSingular;
		this.additionalSuffix = additionalSuffix;
	}

	public CustomDataKey<MethodMetadata> getCustomDataKey() {
		return customDataKey;
	}

	public Object getTagValue(MethodMetadata key) {
		return null;
	}

	public List<MethodMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {

		List<FieldMetadata> fields = getFieldsInterestedIn(memberHoldingTypeDetailsList);
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		Set<JavaSymbolName> fieldNames = new HashSet<JavaSymbolName>();
		JavaSymbolName userDefinedMethodName = getUserDefinedMethod(memberHoldingTypeDetailsList);
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

	/**
	 * This method returns the plural term as per inflector.
	 * ATTENTION: this method does NOT take @RooPlural into account. Use getPlural(..) instead!
	 *
	 * @param term   The term to be pluralized
	 * @param locale Locale
	 * @return pluralized term
	 */
	public String getInflectorPlural(String term, Locale locale) {
		try {
			return Noun.pluralOf(term, locale);
		} catch (RuntimeException re) {
			// Inflector failed (see for example ROO-305), so don't pluralize it
			return term;
		}
	}

	private JavaSymbolName getUserDefinedMethod(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		if (catalystAnnotationType == null || userDefinedNameAttribute == null) {
			return null;
		}
		String suffix = suffixPlural || suffixSingular ? getSuffix(memberHoldingTypeDetailsList, suffixSingular) : "";
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails && !Modifier.isAbstract(memberHoldingTypeDetails.getModifier())) {
				for (AnnotationMetadata annotationMetadata : memberHoldingTypeDetails.getAnnotations()) {
					if (annotationMetadata.getAnnotationType().equals(catalystAnnotationType)) {
						AnnotationAttributeValue<?> annotationAttributeValue = annotationMetadata.getAttribute(userDefinedNameAttribute);
						if (annotationAttributeValue != null) {
							return new JavaSymbolName(annotationAttributeValue.getValue().toString() + suffix);
						}
						break;
					}
				}
			}
		}
		return defaultName == null ? null : new JavaSymbolName(defaultName + suffix);
	}

	private String getSuffix(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, boolean singular) {
		String plural = "";
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails && !Modifier.isAbstract(memberHoldingTypeDetails.getModifier())) {
				if (singular) {
					return memberHoldingTypeDetails.getName().getSimpleTypeName();
				}
				for (AnnotationMetadata annotationMetadata : memberHoldingTypeDetails.getAnnotations()) {
					if (annotationMetadata.getAnnotationType().equals(new JavaType("org.springframework.roo.addon.plural.RooPlural"))) {
						AnnotationAttributeValue<?> annotationAttributeValue = annotationMetadata.getAttribute(new JavaSymbolName("value"));
						if (annotationAttributeValue != null) {
							return annotationAttributeValue.getValue().toString();
						}
						break;
					}
				}
				plural = getInflectorPlural(memberHoldingTypeDetails.getName().getSimpleTypeName(), Locale.ENGLISH);
			}
		}
		return plural;
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
