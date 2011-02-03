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
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;

/**
 * Shell commands for creating classes, interfaces, and enums.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class ClasspathCommands implements CommandMarker {
	@Reference private ProjectOperations projectOperations;
	@Reference private StaticFieldConverter staticFieldConverter;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	protected void activate(ComponentContext context) {
		staticFieldConverter.add(InheritanceType.class);
	}

	protected void deactivate(ComponentContext context) {
		staticFieldConverter.remove(InheritanceType.class);
	}

	@CliAvailabilityIndicator( { "class", "interface", "enum type", "enum constant" })
	public boolean isProjectAvailable() {
		return projectOperations.isProjectAvailable();
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

		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier = modifier |= Modifier.ABSTRACT;
		}

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);

		if (!superclass.equals(new JavaType("java.lang.Object"))) {
			ClassOrInterfaceTypeDetails superclassClassOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(superclass);
			if (superclassClassOrInterfaceTypeDetails != null) {
				typeDetailsBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(superclassClassOrInterfaceTypeDetails));
			}
		}
		
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		if (rooAnnotations) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.serializable.RooSerializable")));
		}
		typeDetailsBuilder.setAnnotations(annotations);

		typeManagementService.generateClassFile(typeDetailsBuilder.build());
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
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
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
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
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
		typeManagementService.addEnumConstant(declaredByMetadataId, fieldName);
	}
}
