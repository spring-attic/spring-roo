package org.springframework.roo.classpath.operations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.Assert;

/**
 * Shell commands for {@link ClasspathOperationsImpl}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class ClasspathCommands implements CommandMarker {
	@Reference private ClasspathOperations classpathOperations;
	@Reference private StaticFieldConverter staticFieldConverter;

	protected void activate(ComponentContext context) {
		staticFieldConverter.add(InheritanceType.class);
	}

	protected void deactivate(ComponentContext context) {
		staticFieldConverter.remove(InheritanceType.class);
	}

	@CliAvailabilityIndicator( { "class", "dod", "test integration", "interface", "enum type", "enum constant" })
	public boolean isProjectAvailable() {
		return classpathOperations.isProjectAvailable();
	}

	@CliAvailabilityIndicator( { "entity", "embeddable" })
	public boolean isPersistentClassAvailable() {
		return classpathOperations.isPersistentClassAvailable();
	}

	@CliCommand(value = "focus", help = "Changes focus to a different type") 
	public void focus(
		@CliOption(key = "class", mandatory = true, optionContext = "update,project", help = "The type to focus on") JavaType typeName) {
	}
	
	@CliCommand(value = "class", help = "Creates a new Java class source file in any project path")
	public void createClass(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") JavaType name,
		@CliOption(key = "rooAnnotations", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should have common Roo annotations") boolean rooAnnotations, 
		@CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "SRC_MAIN_JAVA", specifiedDefaultValue = "SRC_MAIN_JAVA", help = "Source directory to create the class in") Path path,
		@CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") JavaType superclass, 
		@CliOption(key = "abstract", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should be marked as abstract") boolean createAbstract, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);

		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		if (rooAnnotations) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.serializable.RooSerializable")));
		}

		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier = modifier |= Modifier.ABSTRACT;
		}
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);
		if (classpathOperations.getSuperclass(superclass) != null) {
			typeDetailsBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(classpathOperations.getSuperclass(superclass)));
		}
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		typeDetailsBuilder.setAnnotations(annotations);
		classpathOperations.generateClassFile(typeDetailsBuilder.build());
	}

	@CliCommand(value = "embeddable", help = "Creates a new Java class source file with the JPA @Embeddable annotation in SRC_MAIN_JAVA")
	public void createEmbeddedClass(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") JavaType name,
		@CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") boolean serializable, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Embeddable")));
		if (serializable) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.serializable.RooSerializable")));
		}

		int modifier = Modifier.PUBLIC;
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);
		classpathOperations.generateClassFile(typeDetailsBuilder.build());
	}

	@CliCommand(value = "interface", help = "Creates a new Java interface source file in any project path")
	public void createInterface(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the interface to create") JavaType name, 
		@CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "SRC_MAIN_JAVA", specifiedDefaultValue = "SRC_MAIN_JAVA", help = "Source directory to create the interface in") Path path, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);
		classpathOperations.generateClassFile(typeDetailsBuilder.build());
	}

	@CliCommand(value = "enum type", help = "Creates a new Java enum source file in any project path")
	public void createEnum(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the enum to create") JavaType name, 
		@CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "SRC_MAIN_JAVA", specifiedDefaultValue = "SRC_MAIN_JAVA", help = "Source directory to create the enum in") Path path, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.ENUMERATION);
		classpathOperations.generateClassFile(typeDetailsBuilder.build());
	}

	@CliCommand(value = "enum constant", help = "Inserts a new enum constant into an enum")
	public void enumConstant(
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the enum class to receive this field") JavaType name, 
		@CliOption(key = "name", mandatory = true, help = "The name of the constant") JavaSymbolName fieldName, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			// No need to check the "name" as if the class exists it is assumed it is a legal name
			ReservedWords.verifyReservedWordsNotPresent(fieldName);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);
		classpathOperations.addEnumConstant(declaredByMetadataId, fieldName);
	}

	@CliCommand(value = "dod", help = "Creates a new data on demand for the specified entity")
	public void newDod(
		@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The entity which this data on demand class will create and modify as required") JavaType entity, 
		@CliOption(key = "class", mandatory = false, help = "The class which will be created to hold this data on demand provider (defaults to the entity name + 'DataOnDemand')") JavaType clazz, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		Assert.isTrue(isEntityReasonablyNamed(entity), "Cannot create data on demand for an entity named 'Test' or 'TestCase' under any circumstances");

		if (clazz == null) {
			clazz = new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
		}

		classpathOperations.newDod(entity, clazz, Path.SRC_TEST_JAVA);
	}

	private boolean isEntityReasonablyNamed(JavaType entity) {
		return !entity.getSimpleTypeName().startsWith("Test") && !entity.getSimpleTypeName().endsWith("TestCase") && !entity.getSimpleTypeName().endsWith("Test");
	}

	@CliCommand(value = "test integration", help = "Creates a new integration test for the specified entity")
	public void newIntegrationTest(
		@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the entity to create an integration test for") JavaType entity, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		Assert.isTrue(isEntityReasonablyNamed(entity), "Cannot create an integration test for an entity named 'Test' or 'TestCase' under any circumstances");
		
		classpathOperations.newIntegrationTest(entity);
	}

	@CliCommand(value = "entity", help = "Creates a new JPA persistent entity in SRC_MAIN_JAVA")
	public void newPersistenceClassJpa(
		@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "Name of the entity to create") JavaType name, 
		@CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") JavaType superclass, 
		@CliOption(key = "abstract", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Whether the generated class should be marked as abstract") boolean createAbstract, 
		@CliOption(key = "testAutomatically", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for this entity") boolean testAutomatically, 
		@CliOption(key = "table", mandatory = false, help = "The JPA table name to use for this entity") String table, 
		@CliOption(key = "identifierField", mandatory = false, help = "The JPA identifier field name to use for this entity") String identifierField, 
		@CliOption(key = "identifierColumn", mandatory = false, help = "The JPA identifier field column to use for this entity") String identifierColumn, 
		@CliOption(key = "identifierType", mandatory = false, optionContext = "java-lang,project", unspecifiedDefaultValue = "java.lang.Long", specifiedDefaultValue = "java.lang.Long", help = "The data type that will be used for the JPA identifier field (defaults to java.lang.Long)") JavaType identifierType, 
		@CliOption(key = "inheritanceType", mandatory = false, help = "The JPA @Inheritance value") InheritanceType inheritanceType, 
		@CliOption(key = "mappedSuperclass", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Apply @MappedSuperclass for this entity") boolean mappedSuperclass, 
		@CliOption(key = "serializable", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should implement java.io.Serializable") boolean serializable, 
		@CliOption(key = "persistenceUnit", mandatory = false, help = "The persistence unit name to be used in the persistence.xml file") String persistenceUnit,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

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
		if (!isEntityReasonablyNamed(name)) {
			if (permitReservedWords && testAutomatically) {
				throw new IllegalArgumentException("Entity name cannot contain 'Test' or 'TestCase' as you are requesting tests; remove --testAutomatically or rename the proposed entity");
			}
			if (!permitReservedWords) {
				throw new IllegalArgumentException("Entity name rejected as conflicts with test execution defaults; please remove 'Test' and/or 'TestCase'");
			}
		}

		// Produce entity itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);
		List<AnnotationMetadataBuilder> entityAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		entityAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
		entityAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));

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
		if (persistenceUnit != null) {
			rooEntityBuilder.addStringAttribute("persistenceUnit", persistenceUnit);			
		}
		entityAnnotations.add(rooEntityBuilder);

		if (table != null) {
			AnnotationMetadataBuilder tableBuilder = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Table"));
			tableBuilder.addStringAttribute("name", table);
			entityAnnotations.add(tableBuilder);
		}

		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		if (mappedSuperclass) {
			entityAnnotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.MappedSuperclass")));
		} else {
			entityAnnotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Entity")));
		}
		if (serializable) {
			entityAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.serializable.RooSerializable")));
		}
		if (inheritanceType != null) {
			AnnotationMetadataBuilder inheritanceBuilder = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Inheritance"));
			inheritanceBuilder.addEnumAttribute("strategy", new EnumDetails(new JavaType("javax.persistence.InheritanceType"), new JavaSymbolName(inheritanceType.name())));
			entityAnnotations.add(inheritanceBuilder);
			if (inheritanceType == InheritanceType.SINGLE_TABLE) {
				// Theoretically not required based on @DiscriminatorColumn JavaDocs, but Hibernate appears to fail if it's missing
				entityAnnotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.DiscriminatorColumn")));
			}
		}
		
		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier = modifier |= Modifier.ABSTRACT;
		}
		
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
		if (classpathOperations.getSuperclass(superclass) != null) {
			typeDetailsBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(classpathOperations.getSuperclass(superclass)));
		}
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		typeDetailsBuilder.setAnnotations(entityAnnotations);
		classpathOperations.generateClassFile(typeDetailsBuilder.build());
		
		// Create entity identifier class if required
		if (!(identifierType.getPackage().getFullyQualifiedPackageName().startsWith("java.") || identifierType.equals(new JavaType("com.google.appengine.api.datastore.Key")))) {
			createIdentifierClass(identifierType, identifierField, identifierColumn);
		}

		if (testAutomatically) {
			classpathOperations.newIntegrationTest(name);
		}
	}

	private void createIdentifierClass(JavaType identifierType, String identifierField, String identifierColumn) {
		// Produce identifier itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		List<AnnotationMetadataBuilder> identifierAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		identifierAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
		identifierAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.entity.RooIdentifier")));
		
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL, identifierType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(identifierAnnotations);
		classpathOperations.generateClassFile(typeDetailsBuilder.build());
	}
}
