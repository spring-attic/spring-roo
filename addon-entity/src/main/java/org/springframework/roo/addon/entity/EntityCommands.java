package org.springframework.roo.addon.entity;

import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.RooJavaType.ROO_DISPLAY_STRING;
import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.equals.EqualsOperations;
import org.springframework.roo.addon.test.IntegrationTestOperations;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.util.Assert;

/**
 * Shell commands for creating entities, integration tests, and data-on-demand (DoD) classes.
 *
 * @author Alan Stewart
 * @since 1.1.2
 */
@Component
@Service
public class EntityCommands implements CommandMarker {

	// Constants
	private static final AnnotationMetadataBuilder ROO_EQUALS_BUILDER = new AnnotationMetadataBuilder(ROO_EQUALS);
	private static final AnnotationMetadataBuilder ROO_SERIALIZABLE_BUILDER = new AnnotationMetadataBuilder(ROO_SERIALIZABLE);
	private static final AnnotationMetadataBuilder ROO_TO_STRING_BUILDER = new AnnotationMetadataBuilder(ROO_TO_STRING);
	private static final AnnotationMetadataBuilder ROO_JAVA_BEAN_BUILDER = new AnnotationMetadataBuilder(ROO_JAVA_BEAN);
	private static final AnnotationMetadataBuilder ROO_DISPLAY_STRING_BUILDER = new AnnotationMetadataBuilder(ROO_DISPLAY_STRING);

	// Fields
	@Reference private EntityOperations entityOperations;
	@Reference private EqualsOperations equalsOperations;
	@Reference private IntegrationTestOperations integrationTestOperations;

	@CliAvailabilityIndicator( { "entity jpa", "embeddable" })
	public boolean isPersistentClassAvailable() {
		return entityOperations.isPersistentClassAvailable();
	}

	@CliCommand(value = "entity jpa", help = "Creates a new JPA persistent entity in SRC_MAIN_JAVA")
	public void newPersistenceClassJpa(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "Name of the entity to create") final JavaType name,
		@CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") final JavaType superclass,
		@CliOption(key = "abstract", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether the generated class should be marked as abstract") final boolean createAbstract,
		@CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for this entity") final boolean testAutomatically,
		@CliOption(key = "table", mandatory = false, help = "The JPA table name to use for this entity") final String table,
		@CliOption(key = "schema", mandatory = false, help = "The JPA table schema name to use for this entity") final String schema,
		@CliOption(key = "catalog", mandatory = false, help = "The JPA table catalog name to use for this entity") final String catalog,
		@CliOption(key = "identifierField", mandatory = false, help = "The JPA identifier field name to use for this entity") final String identifierField,
		@CliOption(key = "identifierColumn", mandatory = false, help = "The JPA identifier field column to use for this entity") final String identifierColumn,
		@CliOption(key = "identifierType", mandatory = false, optionContext = "java-lang,project", unspecifiedDefaultValue = "java.lang.Long", specifiedDefaultValue = "java.lang.Long", help = "The data type that will be used for the JPA identifier field (defaults to java.lang.Long)") final JavaType identifierType,
		@CliOption(key = "versionField", mandatory = false, help = "The JPA version field name to use for this entity") final String versionField,
		@CliOption(key = "versionColumn", mandatory = false, help = "The JPA version field column to use for this entity") final String versionColumn,
		@CliOption(key = "versionType", mandatory = false, optionContext = "java-lang,project", unspecifiedDefaultValue = "java.lang.Integer", help = "The data type that will be used for the JPA version field (defaults to java.lang.Integer)") final JavaType versionType,
		@CliOption(key = "inheritanceType", mandatory = false, help = "The JPA @Inheritance value") final InheritanceType inheritanceType,
		@CliOption(key = "mappedSuperclass", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Apply @MappedSuperclass for this entity") final boolean mappedSuperclass,
		@CliOption(key = "equals", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement equals and hashCode methods") final boolean equals,
		@CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") final boolean serializable,
		@CliOption(key = "persistenceUnit", mandatory = false, help = "The persistence unit name to be used in the persistence.xml file") final String persistenceUnit,
		@CliOption(key = "transactionManager", mandatory = false, help = "The transaction manager name") final String transactionManager,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
		@CliOption(key = "entityName", mandatory = false, help = "The name used to refer to the entity in queries") final String entityName,
		@CliOption(key = "activeRecord", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Generate CRUD active record methods for this entity") final boolean activeRecord) {
		Assert.isTrue(!identifierType.isPrimitive(), "Identifier type cannot be a primitive");

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}
		if (testAutomatically && createAbstract) {
			// We can't test an abstract class
			throw new IllegalArgumentException("Automatic tests cannot be created for an abstract entity; remove the --testAutomatically or --abstract option");
		}

		// Reject attempts to name the entity "Test", due to possible clashes with data on demand (see ROO-50)
		// We will allow this to happen, though if the user insists on it via --permitReservedWords (see ROO-666)
		if (!BeanInfoUtils.isEntityReasonablyNamed(name)) {
			if (permitReservedWords && testAutomatically) {
				throw new IllegalArgumentException("Entity name cannot contain 'Test' or 'TestCase' as you are requesting tests; remove --testAutomatically or rename the proposed entity");
			}
			if (!permitReservedWords) {
				throw new IllegalArgumentException("Entity name rejected as conflicts with test execution defaults; please remove 'Test' and/or 'TestCase'");
			}
		}

		// Create entity's annotations
		final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
		annotationBuilder.add(ROO_JAVA_BEAN_BUILDER);
		annotationBuilder.add(ROO_TO_STRING_BUILDER);
		annotationBuilder.add(ROO_DISPLAY_STRING_BUILDER);
		annotationBuilder.add(getEntityAnnotationBuilder(table, schema, catalog, identifierField, identifierColumn, identifierType, versionField, versionColumn, versionType, inheritanceType, mappedSuperclass, persistenceUnit, transactionManager, entityName, activeRecord));
		if (equals) {
			annotationBuilder.add(ROO_EQUALS_BUILDER);
			equalsOperations.updateConfiguration();
		}
		if (serializable) {
			annotationBuilder.add(ROO_SERIALIZABLE_BUILDER);
		}

		// Produce the entity itself
		entityOperations.newEntity(name, createAbstract, superclass, annotationBuilder);

		// Create entity identifier class if required
		if (!(identifierType.getPackage().getFullyQualifiedPackageName().startsWith("java.") || identifierType.equals(GAE_DATASTORE_KEY))) {
			entityOperations.newIdentifier(identifierType, identifierField, identifierColumn);
		}

		if (testAutomatically) {
			integrationTestOperations.newIntegrationTest(name);
		}
	}

	/**
	 * Returns a builder for the entity-related annotation to be added to a
	 * newly created JPA entity
	 *
	 * @param table
	 * @param schema
	 * @param catalog
	 * @param identifierField
	 * @param identifierColumn
	 * @param identifierType
	 * @param versionField
	 * @param versionColumn
	 * @param versionType
	 * @param inheritanceType
	 * @param mappedSuperclass
	 * @param persistenceUnit
	 * @param transactionManager
	 * @param entityName
	 * @param activeRecord whether to generate active record CRUD methods for the entity
	 * @return a non-<code>null</code> builder
	 */
	private AnnotationMetadataBuilder getEntityAnnotationBuilder(final String table, final String schema, final String catalog, final String identifierField, final String identifierColumn, final JavaType identifierType, final String versionField, final String versionColumn, final JavaType versionType, final InheritanceType inheritanceType, final boolean mappedSuperclass, final String persistenceUnit, final String transactionManager, final String entityName, final boolean activeRecord) {
		final AnnotationMetadataBuilder entityAnnotationBuilder = new AnnotationMetadataBuilder(getEntityAnnotationType(activeRecord));

		// Attributes that apply to all JPA entities (active record or not)
		if (catalog != null) {
			entityAnnotationBuilder.addStringAttribute("catalog", catalog);
		}
		if (entityName != null) {
			entityAnnotationBuilder.addStringAttribute("entityName", entityName);
		}
		if (identifierColumn != null) {
			entityAnnotationBuilder.addStringAttribute("identifierColumn", identifierColumn);
		}
		if (identifierField != null) {
			entityAnnotationBuilder.addStringAttribute("identifierField", identifierField);
		}
		if (!LONG_OBJECT.equals(identifierType)) {
			entityAnnotationBuilder.addClassAttribute("identifierType", identifierType);
		}
		if (inheritanceType != null) {
			entityAnnotationBuilder.addStringAttribute("inheritanceType", inheritanceType.name());
		}
		if (mappedSuperclass) {
			entityAnnotationBuilder.addBooleanAttribute("mappedSuperclass", mappedSuperclass);
		}
		if (schema != null) {
			entityAnnotationBuilder.addStringAttribute("schema", schema);
		}
		if (table != null) {
			entityAnnotationBuilder.addStringAttribute("table", table);
		}
		if (versionColumn != null && !RooJpaEntity.VERSION_COLUMN_DEFAULT.equals(versionColumn)) {
			entityAnnotationBuilder.addStringAttribute("versionColumn", versionColumn);
		}
		if (versionField != null && !RooJpaEntity.VERSION_FIELD_DEFAULT.equals(versionField)) {
			entityAnnotationBuilder.addStringAttribute("versionField", versionField);
		}
		if (!JavaType.INT_OBJECT.equals(versionType)) {
			entityAnnotationBuilder.addClassAttribute("versionType", versionType);
		}

		// Attributes that only apply to entities with CRUD active record methods
		if (activeRecord) {
			if (persistenceUnit != null) {
				entityAnnotationBuilder.addStringAttribute("persistenceUnit", persistenceUnit);
			}
			if (transactionManager != null) {
				entityAnnotationBuilder.addStringAttribute("transactionManager", transactionManager);
			}
		}

		return entityAnnotationBuilder;
	}

	/**
	 * Returns the type of annotation to put on the entity
	 *
	 * @param activeRecord whether the entity is to have CRUD active record
	 * methods generated
	 * @return a non-<code>null</code> type
	 */
	private JavaType getEntityAnnotationType(final boolean activeRecord) {
		if (activeRecord) {
			return RooJavaType.ROO_ENTITY;
		}
		return RooJavaType.ROO_JPA_ENTITY;
	}

	@CliCommand(value = "embeddable", help = "Creates a new Java class source file with the JPA @Embeddable annotation in SRC_MAIN_JAVA")
	public void createEmbeddableClass(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") final JavaType name,
		@CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") final boolean serializable,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		entityOperations.newEmbeddableClass(name, serializable);
	}
}
