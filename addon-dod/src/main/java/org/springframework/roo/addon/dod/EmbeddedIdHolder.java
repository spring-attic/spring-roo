package org.springframework.roo.addon.dod;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Holder for embedded id attributes
 * 
 * @author Alan Stewart
 * @author Greg Turnquist
 * @since 1.1.3
 */
public class EmbeddedIdHolder {

    // Fields
    private final FieldMetadata embeddedIdField;
    private final List<FieldMetadata> idFields;

    public EmbeddedIdHolder(final FieldMetadata embeddedIdField,
            final List<FieldMetadata> idFields) {
        Assert.notNull(embeddedIdField, "Identifier type required");
        Assert.notNull(idFields, "Fields for "
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
