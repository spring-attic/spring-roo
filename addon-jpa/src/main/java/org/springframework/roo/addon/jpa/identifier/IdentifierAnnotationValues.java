package org.springframework.roo.addon.jpa.identifier;

import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;

/**
 * The values of a {@link RooIdentifier} annotation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class IdentifierAnnotationValues extends AbstractAnnotationValues {

    @AutoPopulate private boolean dbManaged;
    @AutoPopulate private boolean gettersByDefault = true;
    @AutoPopulate private boolean noArgConstructor = true;
    @AutoPopulate private boolean settersByDefault;

    /**
     * Constructor that reads the {@link RooIdentifier} annotation on the given
     * governor
     * 
     * @param governor the governor's metadata (required)
     */
    public IdentifierAnnotationValues(
            final MemberHoldingTypeDetailsMetadataItem<?> governor) {
        super(governor, RooIdentifier.class);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    /**
     * Indicates whether the identifier class is managed by DBRE
     * 
     * @return
     */
    public boolean isDbManaged() {
        return dbManaged;
    }

    /**
     * Indicates whether to generate getters for the id fields
     * 
     * @return
     */
    public boolean isGettersByDefault() {
        return gettersByDefault;
    }

    /**
     * Indicates whether to generate a no-argument constructor for the class
     * 
     * @return
     */
    public boolean isNoArgConstructor() {
        return noArgConstructor;
    }

    /**
     * Indicates whether to generate setters for the id fields
     * 
     * @return
     */
    public boolean isSettersByDefault() {
        return settersByDefault;
    }
}
