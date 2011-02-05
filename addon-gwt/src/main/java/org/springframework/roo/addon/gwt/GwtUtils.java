package org.springframework.roo.addon.gwt;

import hapax.TemplateDataDictionary;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

import java.lang.reflect.Modifier;
import java.util.*;

public class GwtUtils {

	private static MirrorTypeNamingStrategy mirrorTypeNamingStrategy = new DefaultMirrorTypeNamingStrategy();

	public static Map<GwtType, JavaType> getMirrorTypeMap(ProjectMetadata projectMetadata, JavaType governorType) {
		Map<GwtType, JavaType> mirrorTypeMap = new HashMap<GwtType, JavaType>();
		for (GwtType mirrorType : GwtType.values()) {
			mirrorTypeMap.put(mirrorType, mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, governorType));
		}
		return mirrorTypeMap;
	}

	/**
	 * Return the type arg for the client side method, given the domain method return type.
	 * if domainMethodReturnType is List<Integer> or Set<Integer>, returns the same.
	 * if domainMethodReturnType is List<Employee>, return List<EmployeeProxy>
	 *
	 * @param type
	 */
	public static JavaType getGwtSideLeafType(JavaType type, MetadataService metadataService, ProjectMetadata projectMetadata, JavaType governorType) {
		if (isCommonType(type)) {
			return type;
		}

		if (type.isPrimitive()) {
			checkPrimitive(type);
			return convertPrimitiveType(type);
		}

		if (isCollectionType(type)) {
			List<JavaType> args = type.getParameters();
			if (args != null && args.size() == 1) {
				JavaType elementType = args.get(0);
				return new JavaType(type.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(getGwtSideLeafType(elementType, metadataService, projectMetadata, governorType)));
			}
			return type;
		}

		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (isDomainObject(type, ptmd)) {

			if (isEmbeddable(ptmd)) {
				throw new IllegalStateException("GWT does not currently support embedding objects in entities, such as '" + type.getSimpleTypeName() + "' in '" + governorType.getSimpleTypeName() + "'.");
			}

			return getDestinationJavaType(type, GwtType.PROXY, projectMetadata);
		}

		return type;
	}

	public static void addImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
		dataDictionary.addSection("imports").setVariable("import", importDeclaration);
	}

	public static void removeImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
		dataDictionary.getSection("imports").remove(importDeclaration);
	}

	public static void maybeAddImport(TemplateDataDictionary dataDictionary,
	                                  Set<String> importSet, JavaType type) {
		if (!importSet.contains(type.getFullyQualifiedTypeName())) {
			GwtUtils.addImport(dataDictionary, type.getFullyQualifiedTypeName());
			importSet.add(type.getFullyQualifiedTypeName());
		}
	}


	//TODO: this should probably be sitting (is already??) in a lower level util class
	public PhysicalTypeMetadata getPhysicalTypeMetadata(String declaredByMetadataId) {
		return null;//(PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}

	public static void checkPrimitive(JavaType returnType) {
		if (returnType.isPrimitive()) {
			String to = "";
			String from = "";
			if (returnType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
				from = "boolean";
				to = "Boolean";
			}
			if (returnType.equals(JavaType.INT_PRIMITIVE)) {
				from = "int";
				to = "Integer";
			}
			if (returnType.equals(JavaType.BYTE_PRIMITIVE)) {
				from = "byte";
				to = "Byte";
			}
			if (returnType.equals(JavaType.SHORT_PRIMITIVE)) {
				from = "short";
				to = "Short";
			}
			if (returnType.equals(JavaType.FLOAT_PRIMITIVE)) {
				from = "float";
				to = "Float";
			}
			if (returnType.equals(JavaType.DOUBLE_PRIMITIVE)) {
				from = "double";
				to = "Double";
			}
			if (returnType.equals(JavaType.CHAR_PRIMITIVE)) {
				from = "char";
				to = "Character";
			}
			if (returnType.equals(JavaType.LONG_PRIMITIVE)) {
				from = "long";
				to = "Long";
			}

			//throw new IllegalStateException("GWT does not currently support primitive types in an entity. Please change any '" + from + "' entity property types to 'java.lang." + to + "'.");
		}
	}

	public static JavaType convertPrimitiveType(JavaType type) {
		if (type.equals(JavaType.BOOLEAN_PRIMITIVE)) {
			return JavaType.BOOLEAN_OBJECT;
		}
		if (type.equals(JavaType.INT_PRIMITIVE)) {
			return JavaType.INT_OBJECT;
		}
		if (type.equals(JavaType.BYTE_PRIMITIVE)) {
			return JavaType.BYTE_OBJECT;
		}
		if (type.equals(JavaType.SHORT_PRIMITIVE)) {
			return JavaType.SHORT_OBJECT;
		}
		if (type.equals(JavaType.FLOAT_PRIMITIVE)) {
			return JavaType.FLOAT_OBJECT;
		}
		if (type.equals(JavaType.DOUBLE_PRIMITIVE)) {
			return JavaType.DOUBLE_OBJECT;
		}
		if (type.equals(JavaType.CHAR_PRIMITIVE)) {
			return JavaType.CHAR_OBJECT;
		}
		if (type.equals(JavaType.LONG_PRIMITIVE)) {
			return JavaType.LONG_OBJECT;
		}
		return type;
	}

	public static boolean isCommonType(JavaType type) {
		return JavaType.BOOLEAN_OBJECT == type || JavaType.BOOLEAN_PRIMITIVE == type ||
				JavaType.CHAR_OBJECT == type || JavaType.CHAR_PRIMITIVE == type ||
				JavaType.BYTE_OBJECT == type || JavaType.BYTE_PRIMITIVE == type ||
				JavaType.SHORT_OBJECT == type || JavaType.SHORT_PRIMITIVE == type ||
				JavaType.INT_OBJECT == type || JavaType.INT_PRIMITIVE == type ||
				JavaType.LONG_OBJECT == type || JavaType.LONG_PRIMITIVE == type ||
				JavaType.FLOAT_OBJECT == type || JavaType.FLOAT_PRIMITIVE == type ||
				JavaType.DOUBLE_OBJECT == type || JavaType.FLOAT_OBJECT == type ||
				JavaType.VOID_OBJECT == type || JavaType.VOID_PRIMITIVE == type ||
				JavaType.STRING_OBJECT == type;
	}

	public static boolean isDomainObject(JavaType returnType, PhysicalTypeMetadata ptmd) {
		boolean isEnum = ptmd != null
				&& ptmd.getMemberHoldingTypeDetails() != null
				&& ptmd.getMemberHoldingTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;

		return !isEnum
				&& isEntity(ptmd)
				&& !(isRequestFactoryPrimitive(returnType))
				&& !(isCollectionType(returnType))
				&& !isEmbeddable(ptmd);
	}

	public static boolean isEntity(PhysicalTypeMetadata ptmd) {
		if (ptmd != null && ptmd.getMemberHoldingTypeDetails() != null) {
			List<AnnotationMetadata> annotations = ptmd.getMemberHoldingTypeDetails().getAnnotations();
			for (AnnotationMetadata annotation : annotations) {
				if (annotation.getAnnotationType().equals(new JavaType("org.springframework.roo.addon.entity.RooEntity"))) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isEmbeddable(PhysicalTypeMetadata ptmd) {
		if (ptmd != null && ptmd.getMemberHoldingTypeDetails() != null) {
			List<AnnotationMetadata> annotations = ptmd.getMemberHoldingTypeDetails().getAnnotations();
			for (AnnotationMetadata annotation : annotations) {
				if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Embeddable"))) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isCollectionType(JavaType returnType) {
		return returnType.equals(new JavaType("java.util.List"))
				|| returnType.equals(new JavaType("java.util.Set"));
	}

	public static boolean isRequestFactoryPrimitive(JavaType returnType) {
		return returnType.equals(JavaType.BOOLEAN_OBJECT)
				|| returnType.equals(JavaType.INT_OBJECT)
				|| returnType.isPrimitive()
				|| returnType.equals(JavaType.LONG_OBJECT)
				|| returnType.equals(JavaType.STRING_OBJECT)
				|| returnType.equals(JavaType.DOUBLE_OBJECT)
				|| returnType.equals(JavaType.FLOAT_OBJECT)
				|| returnType.equals(JavaType.CHAR_OBJECT)
				|| returnType.equals(JavaType.BYTE_OBJECT)
				|| returnType.equals(JavaType.SHORT_OBJECT)
				|| returnType.equals(new JavaType("java.util.Date"))
				|| returnType.equals(new JavaType("java.math.BigDecimal"));
	}

	public static MethodMetadataBuilder cloneMethod(MethodMetadataBuilder method, String metadataId) {
		MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(metadataId);
		methodMetadataBuilder.setMethodName(method.getMethodName());
		methodMetadataBuilder.setReturnType(method.getReturnType());
		methodMetadataBuilder.setBodyBuilder(method.getBodyBuilder());
		methodMetadataBuilder.setAnnotations(method.getAnnotations());
		if (method.getModifier() == Modifier.PRIVATE) {
			methodMetadataBuilder.setModifier(Modifier.PROTECTED);
		} else if (method.getModifier() == (Modifier.PRIVATE | Modifier.FINAL)) {
			methodMetadataBuilder.setModifier(Modifier.PROTECTED);
		} else {
			methodMetadataBuilder.setModifier(method.getModifier());
		}
		methodMetadataBuilder.setParameterNames(method.getParameterNames());
		methodMetadataBuilder.setParameterTypes(method.getParameterTypes());
		methodMetadataBuilder.setThrowsTypes(method.getThrowsTypes());
		methodMetadataBuilder.setCustomData(method.getCustomData());
		return methodMetadataBuilder;
	}

	public static FieldMetadataBuilder cloneFieldBuilder(FieldMetadataBuilder field, String metadataId) {

		FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(metadataId);
		fieldMetadataBuilder.setFieldName(field.getFieldName());
		fieldMetadataBuilder.setFieldType(field.getFieldType());
		if (field.getModifier() == Modifier.PRIVATE) {
			fieldMetadataBuilder.setModifier(Modifier.PROTECTED);
		} else if (field.getModifier() == (Modifier.PRIVATE | Modifier.FINAL)) {
			fieldMetadataBuilder.setModifier(Modifier.PROTECTED);
		} else {
			fieldMetadataBuilder.setModifier(field.getModifier());
		}
		fieldMetadataBuilder.setAnnotations(field.getAnnotations());
		fieldMetadataBuilder.setCustomData(field.getCustomData());
		fieldMetadataBuilder.setFieldInitializer(field.getFieldInitializer());

		return fieldMetadataBuilder;
	}

	/**
	 * @param mirrorType the mirror class we're producing (required)
	 * @return the MID to the mirror class applicable for the current governor (never null)
	 */
/*
	private String getDestinationMetadataId(MirrorType mirrorType) {
		return PhysicalTypeIdentifier.createIdentifier(mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, governorTypeDetails.getName()), mirrorTypePath);
	}
*/
	public static JavaType getDestinationJavaType(JavaType physicalType, GwtType mirrorType, ProjectMetadata projectMetadata) {
		return mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, physicalType);
	}

	/**
	 * @param mirrorType the mirror class we're producing (required)
	 * @return the Java type the mirror class applicable for the current governor (never null)
	 */
/*	private JavaType getDestinationJavaType(MirrorType mirrorType) {
		return PhysicalTypeIdentifier.getJavaType(getDestinationMetadataId(mirrorType));
	}*/
}
