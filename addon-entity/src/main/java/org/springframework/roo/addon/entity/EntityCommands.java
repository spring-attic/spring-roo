package org.springframework.roo.addon.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.test.IntegrationTestOperations;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
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
	@Reference private EntityOperations entityOperations;
	@Reference private IntegrationTestOperations integrationTestOperations;
	
	@CliAvailabilityIndicator( { "entity", "embeddable" })
	public boolean isPersistentClassAvailable() {
		return entityOperations.isPersistentClassAvailable();
	}

	@CliCommand(value = "entity", help = "Creates a new JPA persistent entity in SRC_MAIN_JAVA")
	public void newPersistenceClassJpa(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "Name of the entity to create") JavaType name, 
		@CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") JavaType superclass, 
		@CliOption(key = "abstract", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether the generated class should be marked as abstract") boolean createAbstract, 
		@CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for this entity") boolean testAutomatically, 
		@CliOption(key = "table", mandatory = false, help = "The JPA table name to use for this entity") String table, 
		@CliOption(key = "schema", mandatory = false, help = "The JPA table schema name to use for this entity") String schema, 
		@CliOption(key = "catalog", mandatory = false, help = "The JPA table catalog name to use for this entity") String catalog, 
		@CliOption(key = "identifierField", mandatory = false, help = "The JPA identifier field name to use for this entity") String identifierField, 
		@CliOption(key = "identifierColumn", mandatory = false, help = "The JPA identifier field column to use for this entity") String identifierColumn, 
		@CliOption(key = "identifierType", mandatory = false, optionContext = "java-lang,project", unspecifiedDefaultValue = "java.lang.Long", specifiedDefaultValue = "java.lang.Long", help = "The data type that will be used for the JPA identifier field (defaults to java.lang.Long)") JavaType identifierType, 
		@CliOption(key = "versionField", mandatory = false, help = "The JPA version field name to use for this entity") String versionField, 
		@CliOption(key = "versionColumn", mandatory = false, help = "The JPA version field column to use for this entity") String versionColumn, 
		@CliOption(key = "inheritanceType", mandatory = false, help = "The JPA @Inheritance value") InheritanceType inheritanceType, 
		@CliOption(key = "mappedSuperclass", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Apply @MappedSuperclass for this entity") boolean mappedSuperclass, 
		@CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") boolean serializable, 
		@CliOption(key = "persistenceUnit", mandatory = false, help = "The persistence unit name to be used in the persistence.xml file") String persistenceUnit,
		@CliOption(key = "transactionManager", mandatory = false, help = "The transaction manager name") String transactionManager,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords,
		@CliOption(key = "entityName", mandatory = false, help = "The name used to refer to the entity in queries") String entityName) {

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
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));

		AnnotationMetadataBuilder rooEntityBuilder = new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.entity.RooEntity"));
		if (identifierField != null) {
			rooEntityBuilder.addStringAttribute("identifierField", identifierField);
		}
		if (identifierColumn != null) {
			rooEntityBuilder.addStringAttribute("identifierColumn", identifierColumn);
		}
		if (!JavaType.LONG_OBJECT.equals(identifierType)) {
			rooEntityBuilder.addClassAttribute("identifierType", identifierType);
		} 
		if (versionField != null && !"version".equals(versionField)) {
			rooEntityBuilder.addStringAttribute("versionField", versionField);
		}
		if (versionColumn != null && !"version".equals(versionColumn)) {
			rooEntityBuilder.addStringAttribute("versionColumn", versionColumn);
		}
		if (persistenceUnit != null) {
			rooEntityBuilder.addStringAttribute("persistenceUnit", persistenceUnit);
		}
		if (transactionManager != null) {
			rooEntityBuilder.addStringAttribute("transactionManager", transactionManager);
		}
		if (mappedSuperclass) {
			rooEntityBuilder.addBooleanAttribute("mappedSuperclass", mappedSuperclass);
		}
		if (table != null) {
			rooEntityBuilder.addStringAttribute("table", table);
		}
		if (schema != null) {
			rooEntityBuilder.addStringAttribute("schema", schema);
		}
		if (catalog != null) {
			rooEntityBuilder.addStringAttribute("catalog", catalog);
		}
		if (inheritanceType != null) {
			rooEntityBuilder.addStringAttribute("inheritanceType", inheritanceType.name());
		}
		if (entityName != null) {
			rooEntityBuilder.addStringAttribute("entityName", entityName);
		}
		annotations.add(rooEntityBuilder);

		if (serializable) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.serializable.RooSerializable")));
		}

		// Produce entity itself
		entityOperations.newEntity(name, createAbstract, superclass, annotations);
		
		// Create entity identifier class if required
		if (!(identifierType.getPackage().getFullyQualifiedPackageName().startsWith("java.") || identifierType.equals(new JavaType("com.google.appengine.api.datastore.Key")))) {
			entityOperations.newIdentifier(identifierType, identifierField, identifierColumn);
		}

		if (testAutomatically) {
			integrationTestOperations.newIntegrationTest(name);
		}
	}

	@CliCommand(value = "embeddable", help = "Creates a new Java class source file with the JPA @Embeddable annotation in SRC_MAIN_JAVA")
	public void createEmbeddableClass(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") JavaType name,
		@CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") boolean serializable, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		entityOperations.newEmbeddableClass(name, serializable);
	}
}
