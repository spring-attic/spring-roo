package org.springframework.roo.classpath.details;

import java.util.List;
import java.util.Set;

import org.springframework.roo.model.JavaSymbolName;

/**
 * Provides information about the different components of a class, interface or
 * enum.
 * <p>
 * As per this interface's extension of {@link MemberHoldingTypeDetails},
 * instances of implementing classes must be immutable.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ClassOrInterfaceTypeDetails extends MemberHoldingTypeDetails {

    /**
     * Indicates whether this class or interface declares a field with the given
     * name
     * 
     * @param fieldName the field name to check for (can be <code>null</code>)
     * @return <code>false</code> if a <code>null</code> field name is given
     */
    boolean declaresField(JavaSymbolName fieldName);

    /**
     * Lists the enum constants this type provides. Always empty except if an
     * enum type.
     * 
     * @return the constants (may be empty, but never null)
     */
    List<JavaSymbolName> getEnumConstants();

    /**
     * @return the explicitly-registered imports this user wishes to have
     *         defined in the type (cannot be null, but may be empty)
     */
    Set<ImportMetadata> getRegisteredImports();

    /**
     * Obtains the superclass if this is a class and it is available.
     * 
     * @return the superclass, if available (null will be returned for
     *         interfaces, or if the class isn't available)
     */
    ClassOrInterfaceTypeDetails getSuperclass();

    /**
     * Indicates whether this class, interface, or enum is abstract
     * 
     * @return see above
     */
    boolean isAbstract();
}
