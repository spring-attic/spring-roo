package org.springframework.roo.classpath.operations;

import java.util.Set;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Classpath-related operations
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface ClasspathOperations {

    /**
     * Creates a new Java class source file in any project path.
     * 
     * @param name the name of the class to create
     * @param rooAnnotations whether the generated class should have common Roo
     *            annotations
     * @param path the source directory in which to create the class
     * @param superclass the superclass (defaults to {@link Object})
     * @param createAbstract whether the generated class should be marked as
     *            abstract
     * @param permitReservedWords whether reserved words are ignored by Roo
     */
    void createClass(final JavaType name, final boolean rooAnnotations,
            final LogicalPath path, final JavaType superclass,
            final boolean createAbstract, final boolean permitReservedWords);

    /**
     * Creates a new constructor in the specified class with the fields
     * provided.
     * <p>
     * If the set of fields is null, a public no-arg constructor will be created
     * if not already present. If fields is not null but empty or if all of the
     * supplied fields do not exist in the class, the method returns silently.
     * 
     * @param name the name of the class (required).
     * @param fields the fields to include in the constructor.
     */
    void createConstructor(final JavaType name, final Set<String> fields);

    /**
     * Creates a new Java enum source file in any project path.
     * 
     * @param name the name of the enum to create
     * @param path the source directory in which to create the enum
     * @param permitReservedWords whether reserved words are ignored by Roo
     */
    void createEnum(final JavaType name, final LogicalPath path,
            final boolean permitReservedWords);

    /**
     * Creates a new Java interface source file in any project path.
     * 
     * @param name the name of the interface to create
     * @param path the source directory in which to create the interface
     * @param permitReservedWords whether reserved words are ignored by Roo
     */
    void createInterface(final JavaType name, final LogicalPath path,
            final boolean permitReservedWords);

    /**
     * Inserts a new enum constant into an enum.
     * 
     * @param name the enum class to receive this field
     * @param fieldName the name of the constant
     * @param permitReservedWords whether reserved words are ignored by Roo
     */
    void enumConstant(final JavaType name, final JavaSymbolName fieldName,
            final boolean permitReservedWords);

    /**
     * Changes the focus to the given type.
     * 
     * @param type the type to focus on (required)
     */
    void focus(final JavaType type);

    boolean isProjectAvailable();
}