package org.springframework.roo.addon.dbre;

import java.io.File;
import java.util.Set;

import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.model.JavaPackage;

/**
 * Provides database reverse engineering operations.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreOperations {

    /**
     * Displays the metadata for the indicated schema on the screen, or writes
     * it to the given file if a filename is specified.
     * 
     * @param schemas the schema(s) to introspect (required)
     * @param file to write to (can be null, in which case the output will
     *            appear on-screen)
     * @param view true if database views are displayed, otherwise false
     */
    void displayDatabaseMetadata(Set<Schema> schemas, File file, boolean view);

    /**
     * Returns whether or not the DBRE commands can be executed.
     * 
     * @return true if the DBRE commands are available to use, otherwise false
     */
    boolean isDbreInstallationPossible();

    /**
     * Introspects the database schema and causes the related entities on disk
     * to be created, updated and deleted.
     * 
     * @param schemas the schema(s) to reverse engineer (required)
     * @param destinationPackage the package in which all entities will be
     *            stored (if not given, the project's top level package)
     * @param testAutomatically whether to create automatic integration tests
     *            for generated entities
     * @param view true if database views are displayed, otherwise false
     * @param includeTables the set of tables to include in reverse engineering.
     * @param excludeTables the set of tables to exclude from reverse
     *            engineering
     * @param includeNonPortableAttributes whether or not to include
     *            non-portable JPA @Column attributes such as 'columnDefinition'
     * @param disableVersionFields whether or not to disable a table's version
     *            column
     * @param disableGeneratedIdentifiers whether or not to disable the
     *            identifier auto generation value
     * @param activeRecord whether to generate CRUD active record methods for
     *            each entity
     * @param repository whether to generate a service layer for each entity
     * @param service whether to generate a repository layer for each entity
     */
    void reverseEngineerDatabase(Set<Schema> schemas,
            JavaPackage destinationPackage, boolean testAutomatically,
            boolean view, Set<String> includeTables, Set<String> excludeTables,
            boolean includeNonPortableAttributes, boolean disableVersionFields,
            boolean disableGeneratedIdentifiers, boolean activeRecord,
            boolean repository, boolean service);
}