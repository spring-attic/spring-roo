package org.springframework.roo.classpath.operations;

import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.RooJavaType.ROO_DISPLAY_NAME;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_SERIALIZABLE;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.Assert;

/**
 * OSGi implementation of {@link ClasspathOperations}.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ClasspathOperationsImpl implements ClasspathOperations {

	// Fields
	@Reference private MetadataService metadataService;
	@Reference private StaticFieldConverter staticFieldConverter;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	protected void activate(final ComponentContext context) {
		staticFieldConverter.add(InheritanceType.class);
	}

	protected void deactivate(final ComponentContext context) {
		staticFieldConverter.remove(InheritanceType.class);
	}

	public void focus(final JavaType type) {
		Assert.notNull(type, "Specify the type to focus on");
		final String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(type);
		final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Class " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier) + " does not exist");
	}

	public void createClass(final JavaType name, final boolean rooAnnotations, final Path path, final JavaType superclass, final boolean createAbstract, final boolean permitReservedWords) {
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);

		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier |= Modifier.ABSTRACT;
		}

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);

		if (!superclass.equals(OBJECT)) {
			ClassOrInterfaceTypeDetails superclassClassOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(superclass);
			if (superclassClassOrInterfaceTypeDetails != null) {
				typeDetailsBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(superclassClassOrInterfaceTypeDetails));
			}
		}

		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);

		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		if (rooAnnotations) {
			annotations.add(new AnnotationMetadataBuilder(ROO_JAVA_BEAN));
			annotations.add(new AnnotationMetadataBuilder(ROO_TO_STRING));
			annotations.add(new AnnotationMetadataBuilder(ROO_SERIALIZABLE));
			annotations.add(new AnnotationMetadataBuilder(ROO_DISPLAY_NAME));
		}
		typeDetailsBuilder.setAnnotations(annotations);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
	}

	public void createInterface(final JavaType name, final Path path, final boolean permitReservedWords) {
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
	}

	public void createEnum(final JavaType name, final Path path, final boolean permitReservedWords) {
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.ENUMERATION);
		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());
	}

	public void enumConstant(final JavaType name, final JavaSymbolName fieldName, final boolean permitReservedWords) {
		if (!permitReservedWords) {
			// No need to check the "name" as if the class exists it is assumed it is a legal name
			ReservedWords.verifyReservedWordsNotPresent(fieldName);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);

		typeManagementService.addEnumConstant(declaredByMetadataId, fieldName);
	}
}
