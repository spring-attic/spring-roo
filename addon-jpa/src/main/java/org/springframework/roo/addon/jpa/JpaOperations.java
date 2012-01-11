package org.springframework.roo.addon.jpa;

import java.util.List;
import java.util.SortedSet;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;

/**
 * Provides JPA configuration and entity operations.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
public interface JpaOperations extends Feature {

    /**
     * This method is responsible for managing all JPA related artifacts
     * (META-INF/persistence.xml, applicationContext.xml, database.properties
     * and the project pom.xml)
     * 
     * @param ormProvider the ORM provider selected (Hibernate, OpenJPA,
     *            EclipseLink)
     * @param database the database (HSQL, H2, MySql, etc)
     * @param jndi the JNDI datasource
     * @param applicationId the Google App Engine application identifier.
     *            Defaults to the project's name if not specified.
     * @param hostName the host name where the database is
     * @param databaseName the name of the database
     * @param userName the username to connect to the database
     * @param password the password to connect to the database
     * @param transactionManager the transaction manager name defined in the
     *            applicationContext.xml file
     * @param persistenceUnit the name of the persistence unit defined in the
     *            persistence.xml file
     * @param moduleName
     */
    void configureJpa(OrmProvider ormProvider, JdbcDatabase database,
            String jndi, String applicationId, String hostName,
            String databaseName, String userName, String password,
            String transactionManager, String persistenceUnit, String moduleName);

    SortedSet<String> getDatabaseProperties();

    boolean hasDatabaseProperties();

    /**
     * Indicates whether JPA can be installed in the currently focused module.
     * 
     * @return <code>false</code> if no module has the focus
     */
    boolean isJpaInstallationPossible();

    /**
     * Checks for the existence the META-INF/persistence.xml
     * 
     * @return true if the META-INF/persistence.xml exists, otherwise false
     */
    boolean isPersistentClassAvailable();

    /**
     * Creates a new JPA embeddable class.
     * 
     * @param name the name of the embeddable class (required)
     * @param serializable whether the class implements
     *            {@link java.io.Serializable}
     */
    void newEmbeddableClass(JavaType name, boolean serializable);

    /**
     * Creates a new entity.
     * 
     * @param name the entity name (required)
     * @param createAbstract indicates whether the entity will be an abstract
     *            class
     * @param superclass the super class of the entity
     * @param annotations the entity's annotations
     */
    void newEntity(JavaType name, boolean createAbstract, JavaType superclass,
            List<AnnotationMetadataBuilder> annotations);

    /**
     * Creates a new JPA identifier class.
     * 
     * @param identifierType the identifier type
     * @param identifierField the identifier field name
     * @param identifierColumn the identifier column name
     */
    void newIdentifier(JavaType identifierType, String identifierField,
            String identifierColumn);
}