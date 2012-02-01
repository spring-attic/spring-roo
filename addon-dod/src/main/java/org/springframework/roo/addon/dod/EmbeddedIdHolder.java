package org.springframework.roo.addon.dod;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Holder for embedded id attributes
 * 
 * @author Alan Stewart
 * @author Greg Turnquist
 * @since 1.1.3
 */
public class EmbeddedIdHolder {

    private final FieldMetadata embeddedIdField;
    private final List<FieldMetadata> idFields;

    public EmbeddedIdHolder(final FieldMetadata embeddedIdField,
            final List<FieldMetadata> idFields) {
        Validate.notNull(embeddedIdField, "Identifier type required");
        Validate.notNull(idFields, "Fields for "
                + embeddedIdField.getFieldType().getFullyQualifiedTypeName()
                + " required");
        this.embeddedIdField = embeddedIdField;
        this.idFields = idFields;
    }

    public FieldMetadata getEmbeddedIdField() {
        return embeddedIdField;
    }

    public JavaSymbolName getEmbeddedIdMutator() {
        return new JavaSymbolName("set"
                + StringUtils.capitalize(embeddedIdField.getFieldName()
                        .getSymbolName()));
    }

    public List<FieldMetadata> getIdFields() {
        return idFields;
    }
}
