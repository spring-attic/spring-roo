package org.springframework.roo.addon.plural;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jvnet.inflector.Noun;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooPlural}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PluralMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = PluralMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType PLURAL_ANNOTATION_TYPE = new JavaType(RooPlural.class.getName());

	// From annotation
	@AutoPopulate private String value = "";

	// Cache
	private Map<String, String> cache;

	public PluralMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, PLURAL_ANNOTATION_TYPE);
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Compute the plural form, if needed
		if ("".equals(this.value)) {
			value = getInflectorPlural(destination.getSimpleTypeName(), Locale.ENGLISH);
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * This method returns the plural term as per inflector.
	 * ATTENTION: this method does NOT take @RooPlural into account. Use getPlural(..) instead!
	 * 
	 * @param term The term to be pluralized
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

	/**
	 * @return the plural of the type name
	 */
	public String getPlural() {
		return value;
	}

	/**
	 * @param field the field to obtain plural details for (required)
	 * @return a guaranteed plural, computed via an annotation or Inflector (never returns null or an empty string)
	 */
	public String getPlural(FieldMetadata field) {
		Assert.notNull(field, "Field required");
		// Obtain the plural from the cache, if available
		String symbolName = field.getFieldName().getSymbolName();
		if (cache != null && cache.containsKey(symbolName)) {
			return cache.get(symbolName);
		}

		// We need to build the plural
		String thePlural = "";
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), PLURAL_ANNOTATION_TYPE);
		if (annotation != null) {
			// Use the plural the user defined via the annotation
			AnnotationAttributeValue<?> attribute = annotation.getAttribute(new JavaSymbolName("value"));
			if (attribute != null) {
				thePlural = attribute.getValue().toString();
			}
		}
		if ("".equals(thePlural)) {
			// Manually compute the plural, as the user did not provided one
			thePlural = getInflectorPlural(symbolName, Locale.ENGLISH);
		}
		if (cache == null) {
			// Create the cache (we defer this in case there is no field plural retrieval ever required for this instance)
			cache = new HashMap<String, String>();
		}
		// Populate the cache for next time
		cache.put(symbolName, thePlural);
		return thePlural;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("plural", getPlural());
		tsc.append("cachedLookups", cache == null ? "[None]" : cache.keySet().toString());
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
