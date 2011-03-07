package org.springframework.roo.addon.gwt;

import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.AbstractIdentifiableAnnotatedJavaStructureBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility methods used in the GWT Add-On.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public class GwtUtils {
	private static Logger logger = HandlerUtils.getLogger(GwtUtils.class);

	private GwtUtils() {
	}

	public static Map<GwtType, JavaType> getMirrorTypeMap(ProjectMetadata projectMetadata, JavaType governorType) {
		Map<GwtType, JavaType> mirrorTypeMap = new HashMap<GwtType, JavaType>();
		for (GwtType mirrorType : GwtType.values()) {
			mirrorTypeMap.put(mirrorType, convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, governorType));
		}
		return mirrorTypeMap;
	}

	public static boolean isRequestMethod(EntityMetadata entityMetadata, MethodMetadata methodMetadata) {
		return isOneMethodsEqual(methodMetadata, entityMetadata.getFindAllMethod(), entityMetadata.getFindMethod(), entityMetadata.getFindEntriesMethod(), entityMetadata.getCountMethod(), entityMetadata.getPersistMethod(), entityMetadata.getRemoveMethod());
	}

	public static JavaType convertGovernorTypeNameIntoKeyTypeName(GwtType type, ProjectMetadata projectMetadata, JavaType governorTypeName) {
		String destinationPackage = type.getPath().packageName(projectMetadata);
		String typeName;
		if (type.isMirrorType()) {
			String simple = governorTypeName.getSimpleTypeName();
			typeName = destinationPackage + "." + simple + type.getSuffix();
		} else {
			typeName = destinationPackage + "." + type.getTemplate();
		}
		return new JavaType(typeName);
	}

	public static boolean hasRequiredEntityMethods(EntityMetadata entityMetadata) {
		String typeName = entityMetadata.getMemberHoldingTypeDetails().getName().getFullyQualifiedTypeName();
		if (entityMetadata.getFindAllMethod() == null) {
			logger.severe("GWT support requires that a proxied entity has a findAll method for type " + typeName);
			return false;
		}
		if (entityMetadata.getFindEntriesMethod() == null) {
			logger.severe("GWT support requires that a proxied entity has a findEntries method for type " + typeName);
			return false;
		}
		if (entityMetadata.getCountMethod() == null) {
			logger.severe("GWT support requires that a proxied entity has a count method for type " + typeName);
			return false;
		}
		if (entityMetadata.getPersistMethod() == null) {
			logger.severe("GWT support requires that a proxied entity has a persist method for type " + typeName);
			return false;
		}
		if (entityMetadata.getRemoveMethod() == null) {
			logger.severe("GWT support requires that a proxied entity has a remove method for type " + typeName);
			return false;
		}
		if (entityMetadata.getVersionAccessor() == null) {
			logger.severe("GWT support requires that a proxied entity has an @Version field accessor method for type " + typeName);
			return false;
		}
		if (entityMetadata.getIdentifierAccessor() == null) {
			logger.severe("GWT support requires that a proxied entity has an @Id field accessor method for type " + typeName);
			return false;
		}
		return true;
	}

	private static boolean isOneMethodsEqual(MethodMetadata m1, MethodMetadata... m2) {
		for (MethodMetadata m : m2) {
			if (areMethodsEqual(m1, m)) {
				return true;
			}
		}
		return false;
	}

	private static boolean areMethodsEqual(MethodMetadata m1, MethodMetadata m2) {
		return m1.getMethodName().equals(m2.getMethodName()) && AnnotatedJavaType.convertFromAnnotatedJavaTypes(m1.getParameterTypes()).equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(m2.getParameterTypes()));
	}

	public static JavaType getDestinationJavaType(GwtType type, ProjectMetadata projectMetadata) {
		return new JavaType(GwtUtils.getFullyQualifiedTypeName(type, projectMetadata));
	}

	public static String getFullyQualifiedTypeName(GwtType gwtType, ProjectMetadata projectMetadata) {
		return gwtType.getPath().packageName(projectMetadata) + "." + gwtType.getTemplate();
	}

	public static <T extends AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure>> T convertModifier(T builder) {
		if (Modifier.isPrivate(builder.getModifier())) {
			builder.setModifier(Modifier.PROTECTED);
		}
		return builder;
	}

	public static void checkPrimitive(JavaType type) {
		if (type.isPrimitive() && !JavaType.VOID_PRIMITIVE.equals(type)) {
			String to = type.getSimpleTypeName();
			String from = to.toLowerCase();
			throw new IllegalStateException("GWT does not currently support primitive types in an entity. Please change any '" + from + "' entity property types to 'java.lang." + to + "'.");
		}
	}

	public static boolean isMappable(ClassOrInterfaceTypeDetails governorTypeDetails, EntityMetadata entityMetadata) {
		if (entityMetadata == null) {
			return false;
		}
		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			return false;
		}
		if (governorTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
			return entityMetadata.isValid() && GwtUtils.hasRequiredEntityMethods(entityMetadata);
		}
		if (governorTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
			return false;
		}
		return false;
	}

	public static JavaType convertPrimitiveType(JavaType type) {
		if (type != null && !JavaType.VOID_PRIMITIVE.equals(type) && type.isPrimitive()) {
			return new JavaType(type.getFullyQualifiedTypeName());
		}
		return type;
	}

	public static boolean isPublicAccessor(MethodMetadata method) {
		return Modifier.isPublic(method.getModifier()) && !method.getReturnType().equals(JavaType.VOID_PRIMITIVE) && method.getParameterTypes().size() == 0 && (method.getMethodName().getSymbolName().startsWith("get"));
	}

	public static boolean isAllowableReturnType(MethodMetadata method) {
		return isAllowableReturnType(method.getReturnType());
	}

	public static boolean isAllowableReturnType(JavaType type) {
		return isCommonType(type) || (isCollectionType(type) && type.getParameters().size() == 1 && isAllowableReturnType(type.getParameters().get(0)));
	}

	public static boolean isCommonType(JavaType type) {
		return JavaType.BOOLEAN_OBJECT.equals(type) ||
				JavaType.CHAR_OBJECT.equals(type) ||
				JavaType.BYTE_OBJECT.equals(type) ||
				JavaType.SHORT_OBJECT.equals(type) ||
				JavaType.INT_OBJECT.equals(type) ||
				JavaType.LONG_OBJECT.equals(type) ||
				JavaType.FLOAT_OBJECT.equals(type) ||
				JavaType.DOUBLE_OBJECT.equals(type) ||
				JavaType.STRING_OBJECT.equals(type) ||
				new JavaType("java.util.Date").equals(type) ||
				new JavaType("java.math.BigDecimal").equals(type) ||
				type.isPrimitive() && !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(type.getFullyQualifiedTypeName());
	}

	public static boolean isDomainObject(JavaType returnType, PhysicalTypeMetadata ptmd) {
		return !isEnum(ptmd)
				&& isEntity(ptmd)
				&& !(isRequestFactoryCompatible(returnType))
				&& !(isCollectionType(returnType))
				&& !isEmbeddable(ptmd);
	}

	public static boolean isEnum(PhysicalTypeMetadata ptmd) {
		return ptmd != null && ptmd.getMemberHoldingTypeDetails() != null && ptmd.getMemberHoldingTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}

	public static boolean isEntity(PhysicalTypeMetadata ptmd) {
		if (ptmd == null) {
			return false;
		}
		AnnotationMetadata annotationMetadata = MemberFindingUtils.getDeclaredTypeAnnotation(ptmd.getMemberHoldingTypeDetails(), new JavaType(RooEntity.class.getName()));
		return annotationMetadata != null;
	}

	public static boolean isEmbeddable(PhysicalTypeMetadata ptmd) {
		if (ptmd == null) {
			return false;
		}
		AnnotationMetadata annotationMetadata = MemberFindingUtils.getDeclaredTypeAnnotation(ptmd.getMemberHoldingTypeDetails(), new JavaType("javax.persistence.Embeddable"));
		return annotationMetadata != null;
	}

	public static boolean isCollectionType(JavaType returnType) {
		return returnType.getFullyQualifiedTypeName().equals("java.util.List") || returnType.getFullyQualifiedTypeName().equals("java.util.Set");
	}

	public static boolean isRequestFactoryCompatible(JavaType type) {
		return isCommonType(type) || isCollectionType(type);
	}

	public static boolean methodBuildersEqual(MethodMetadataBuilder m1, MethodMetadataBuilder m2) {
		boolean match = false;
		if (m1.getMethodName().equals(m2.getMethodName())) {
			match = true;
		}
		return match;
	}

	/**
	 * @param physicalType
	 * @param mirrorType      the mirror class we're producing (required)
	 * @param projectMetadata
	 * @return the MID to the mirror class applicable for the current governor (never null)
	 */
	public static JavaType getDestinationJavaType(JavaType physicalType, GwtType mirrorType, ProjectMetadata projectMetadata) {
		return convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, physicalType);
	}

	public static HashMap<JavaSymbolName, JavaType> resolveTypes(JavaType generic, JavaType typed) {
		HashMap<JavaSymbolName, JavaType> typeMap = new HashMap<JavaSymbolName, JavaType>();
		boolean typeCountMatch = generic.getParameters().size() == typed.getParameters().size();
		Assert.isTrue(typeCountMatch, "Type count must match.");

		int i = 0;
		for (JavaType genericParamType : generic.getParameters()) {
			typeMap.put(genericParamType.getArgName(), typed.getParameters().get(i));
			i++;
		}
		return typeMap;
	}
}
