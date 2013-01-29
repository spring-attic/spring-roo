package org.springframework.roo.addon.plural;

import static org.springframework.roo.model.RooJavaType.ROO_PLURAL;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jvnet.inflector.Noun;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooPlural}.
 * <p>
 * Note that although this class extends
 * {@link AbstractItdTypeDetailsProvidingMetadataItem}, it never adds anything
 * to the ITD builder, hence it never generates an ITD source file.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class PluralMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = PluralMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    /**
     * Creates a plural identifier for the given type in the given path.
     * 
     * @param javaType the type for which to create the identifier (required)
     * @param path the path containing the type (required)
     * @return a valid plural metadata instance ID
     */
    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private Map<String, String> cache;
    private String plural;

    public PluralMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final PluralAnnotationValues pluralAnnotation) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(isValid(identifier), "Metadata id '%s' is invalid",
                identifier);

        if (!isValid()) {
            return;
        }

        plural = getPlural(pluralAnnotation);
    }

    @Override
    public boolean equals(final Object obj) {
        // We override equals because we overrode hashCode, see that method
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PluralMetadata)) {
            return false;
        }
        final PluralMetadata other = (PluralMetadata) obj;
        return StringUtils.equals(plural, other.getPlural());
    }

    /**
     * This method returns the plural term as per inflector. ATTENTION: this
     * method does NOT take @RooPlural into account. Use getPlural(..) instead!
     * 
     * @param term The term to be pluralized
     * @param locale Locale
     * @return pluralized term
     */
    public String getInflectorPlural(final String term, final Locale locale) {
        try {
            return Noun.pluralOf(term, locale);
        }
        catch (final RuntimeException re) {
            // Inflector failed (see for example ROO-305), so don't pluralize it
            return term;
        }
    }

    /**
     * @return the plural of the type name
     */
    public String getPlural() {
        return plural;
    }

    /**
     * @param field the field to obtain plural details for (required)
     * @return a guaranteed plural, computed via an annotation or Inflector
     *         (never returns null or an empty string)
     */
    public String getPlural(final FieldMetadata field) {
        Validate.notNull(field, "Field required");
        // Obtain the plural from the cache, if available
        final String symbolName = field.getFieldName().getSymbolName();
        if (cache != null && cache.containsKey(symbolName)) {
            return cache.get(symbolName);
        }

        // We need to build the plural
        String thePlural = "";
        final AnnotationMetadata annotation = MemberFindingUtils
                .getAnnotationOfType(field.getAnnotations(), ROO_PLURAL);
        if (annotation != null) {
            // Use the plural the user defined via the annotation
            final AnnotationAttributeValue<?> attribute = annotation
                    .getAttribute(new JavaSymbolName("value"));
            if (attribute != null) {
                thePlural = attribute.getValue().toString();
            }
        }
        if ("".equals(thePlural)) {
            // Manually compute the plural, as the user did not provided one
            thePlural = getInflectorPlural(symbolName, Locale.ENGLISH);
        }
        if (cache == null) {
            // Create the cache (we defer this in case there is no field plural
            // retrieval ever required for this instance)
            cache = new HashMap<String, String>();
        }

        // Populate the cache for next time
        cache.put(symbolName, thePlural);

        return thePlural;
    }

    private String getPlural(final PluralAnnotationValues pluralAnnotation) {
        if (StringUtils.isNotBlank(pluralAnnotation.getValue())) {
            return pluralAnnotation.getValue();
        }
        return getInflectorPlural(destination.getSimpleTypeName(),
                Locale.ENGLISH);
    }

    @Override
    public int hashCode() {
        /*
         * We override hashCode because the superclass' implementation compares
         * the contents of the ITD builder, and this class never modifies that
         * builder; meaning that all instances have the same hash code.
         * ITD-generating metadata providers like this one rely on the hash code
         * changing when the underlying metadata (in our case the plural)
         * changes.
         */
        return plural == null ? 0 : plural.hashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("plural", getPlural());
        builder.append("cachedLookups", cache == null ? "[None]" : cache
                .keySet().toString());
        return builder.toString();
    }
}
