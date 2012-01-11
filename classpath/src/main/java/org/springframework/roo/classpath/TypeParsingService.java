package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

public interface TypeParsingService {

    /**
     * Returns the compilation unit contents that represents the passed class or
     * interface details. This is useful if an add-on requires a compilation
     * unit representation but doesn't wish to cause that representation to be
     * emitted to disk via {@link TypeManagementService}. One concrete time this
     * is useful is when an add-on wishes to emulate an ITD-like model for an
     * external system that cannot support ITDs and may wish to insert a custom
     * header etc before writing it to disk.
     * 
     * @param cid a parsed representation of a class or interface (required)
     * @return a valid Java compilation unit for the passed object (never null
     *         or empty)
     */
    String getCompilationUnitContents(ClassOrInterfaceTypeDetails cid);

    /**
     * Builds a {@link ClassOrInterfaceTypeDetails} object that represents the
     * requested {@link org.springframework.roo.model.JavaType} from the type at
     * the passed in type path.
     * 
     * @param fileIdentifier the location of the type to be parsed (required)
     * @param declaredByMetadataId the metadata ID that should be used in the
     *            returned object (required)
     * @param javaType the Java type to locate in the compilation unit and parse
     *            (required)
     * @return a parsed representation of the requested type from the passed
     *         compilation unit (never null)
     */
    ClassOrInterfaceTypeDetails getTypeAtLocation(String fileIdentifier,
            String declaredByMetadataId, JavaType javaType);

    /**
     * Builds a {@link ClassOrInterfaceTypeDetails} object that represents the
     * requested {@link org.springframework.roo.model.JavaType} from the passed
     * compilation unit text. This is useful if an add-on wishes to parse some
     * arbitrary compilation unit contents it acquired from outside the user
     * project, such as a template that ships with the add-on. The add-on can
     * subsequently modify the returned object (via the builder) and eventually
     * write the final version to the user's project. This therefore allows more
     * elegant add-on usage patterns, as they need not write "stub" compilation
     * units into a user project simply to parse them for subsequent re-writing.
     * 
     * @param typeContents the text of a legal Java compilation unit (required)
     * @param declaredByMetadataId the metadata ID that should be used in the
     *            returned object (required)
     * @param javaType the Java type to locate in the compilation unit and parse
     *            (required)
     * @return a parsed representation of the requested type from the passed
     *         compilation unit (never null)
     */
    ClassOrInterfaceTypeDetails getTypeFromString(String typeContents,
            String declaredByMetadataId, JavaType javaType);
}
