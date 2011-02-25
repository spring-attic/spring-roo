package org.springframework.roo.addon.gwt;

import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.AbstractIdentifiableAnnotatedJavaStructureBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility methods used in the GWT Add-On.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public class GwtUtils {
	private static GwtTypeNamingStrategy gwtTypeNamingStrategy = new DefaultGwtTypeNamingStrategy();
	private static Logger logger = HandlerUtils.getLogger(GwtUtils.class);

	private GwtUtils() {
	}

	public static Map<GwtType, JavaType> getMirrorTypeMap(ProjectMetadata projectMetadata, JavaType governorType) {
		Map<GwtType, JavaType> mirrorTypeMap = new HashMap<GwtType, JavaType>();
		for (GwtType mirrorType : GwtType.values()) {
			mirrorTypeMap.put(mirrorType, gwtTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, governorType));
		}
		return mirrorTypeMap;
	}

	public static boolean isRequestMethod(EntityMetadata entityMetadata, MethodMetadata methodMetadata) {
		return isOneMethodsEqual(methodMetadata, entityMetadata.getFindAllMethod(), entityMetadata.getFindMethod(), entityMetadata.getFindEntriesMethod(), entityMetadata.getCountMethod(), entityMetadata.getPersistMethod(), entityMetadata.getRemoveMethod(), entityMetadata.getVersionAccessor(), entityMetadata.getIdentifierAccessor());
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

	private static <T extends AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure>> T convertModifier(T builder) {
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
		return gwtTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, physicalType);
	}

	private static HashMap<JavaSymbolName, JavaType> resolveTypes(JavaType generic, JavaType typed) {
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

	private static ClassOrInterfaceTypeDetailsBuilder createAbstractBuilder(ClassOrInterfaceTypeDetailsBuilder concreteClass, List<MemberHoldingTypeDetails> extendsTypesDetails) {
		JavaType concreteType = concreteClass.getName();
		String abstractName = concreteType.getSimpleTypeName() + "_Roo_Gwt";
		abstractName = concreteType.getPackage().getFullyQualifiedPackageName() + '.' + abstractName;
		JavaType abstractType = new JavaType(abstractName);
		String abstractId = PhysicalTypeIdentifier.createIdentifier(abstractType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractId);
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
		builder.setName(abstractType);
		builder.setModifier(Modifier.ABSTRACT | Modifier.PUBLIC);
		builder.getExtendsTypes().addAll(concreteClass.getExtendsTypes());
		builder.getRegisteredImports().addAll(concreteClass.getRegisteredImports());

		for (MemberHoldingTypeDetails extendsTypeDetails : extendsTypesDetails) {
			for (ConstructorMetadata constructorMetadata : extendsTypeDetails.getDeclaredConstructors()) {
				ConstructorMetadataBuilder abstractConstructor = new ConstructorMetadataBuilder(abstractId);
				abstractConstructor.setModifier(constructorMetadata.getModifier());

				HashMap<JavaSymbolName, JavaType> typeMap = resolveTypes(extendsTypeDetails.getName(), concreteClass.getExtendsTypes().get(0));

				for (AnnotatedJavaType type : constructorMetadata.getParameterTypes()) {
					JavaType newType = type.getJavaType();
					if (type.getJavaType().getParameters().size() > 0) {
						ArrayList<JavaType> paramTypes = new ArrayList<JavaType>();
						for (JavaType typeType : type.getJavaType().getParameters()) {
							JavaType typeParam = typeMap.get(new JavaSymbolName(typeType.toString()));
							if (typeParam != null) {
								paramTypes.add(typeParam);
							}
						}
						newType = new JavaType(type.getJavaType().getFullyQualifiedTypeName(), type.getJavaType().getArray(), type.getJavaType().getDataType(), type.getJavaType().getArgName(), paramTypes);
					}
					abstractConstructor.getParameterTypes().add(new AnnotatedJavaType(newType, null));
				}
				abstractConstructor.setParameterNames(constructorMetadata.getParameterNames());

				InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
				bodyBuilder.newLine().indent().append("super(");

				int i = 0;
				for (JavaSymbolName paramName : abstractConstructor.getParameterNames()) {
					bodyBuilder.append(" ").append(paramName.getSymbolName());
					if (abstractConstructor.getParameterTypes().size() > i + 1) {
						bodyBuilder.append(", ");
					}
					i++;
				}

				bodyBuilder.append(");");

				bodyBuilder.newLine().indentRemove();
				abstractConstructor.setBodyBuilder(bodyBuilder);
				builder.getDeclaredConstructors().add(abstractConstructor);
			}
		}
		return builder;
	}

	public static List<ClassOrInterfaceTypeDetails> buildType(GwtType destType, ClassOrInterfaceTypeDetails templateClass, List<MemberHoldingTypeDetails> extendsTypes) {
		try {
			//A type may consist of a concrete type which depend on
			List<ClassOrInterfaceTypeDetails> types = new ArrayList<ClassOrInterfaceTypeDetails>();
			ClassOrInterfaceTypeDetailsBuilder templateClassBuilder = new ClassOrInterfaceTypeDetailsBuilder(templateClass);

			if (destType.isCreateAbstract()) {
				ClassOrInterfaceTypeDetailsBuilder abstractClassBuilder = createAbstractBuilder(templateClassBuilder, extendsTypes);

				ArrayList<FieldMetadataBuilder> fieldsToRemove = new ArrayList<FieldMetadataBuilder>();
				for (JavaSymbolName fieldName : destType.getWatchedFieldNames()) {
					for (FieldMetadataBuilder fieldBuilder : templateClassBuilder.getDeclaredFields()) {
						if (fieldBuilder.getFieldName().equals(fieldName)) {
							FieldMetadataBuilder abstractFieldBuilder = new FieldMetadataBuilder(abstractClassBuilder.getDeclaredByMetadataId(), fieldBuilder.build());
							abstractClassBuilder.addField(convertModifier(abstractFieldBuilder));
							fieldsToRemove.add(fieldBuilder);
							break;
						}
					}
				}

				templateClassBuilder.getDeclaredFields().removeAll(fieldsToRemove);

				ArrayList<MethodMetadataBuilder> methodsToRemove = new ArrayList<MethodMetadataBuilder>();
				for (JavaSymbolName methodName : destType.getWatchedMethods().keySet()) {
					for (MethodMetadataBuilder methodBuilder : templateClassBuilder.getDeclaredMethods()) {
						if (methodBuilder.getMethodName().equals(methodName)) {
							if (destType.getWatchedMethods().get(methodName).containsAll(AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodBuilder.getParameterTypes()))) {
								MethodMetadataBuilder abstractMethodBuilder = new MethodMetadataBuilder(abstractClassBuilder.getDeclaredByMetadataId(), methodBuilder.build());
								abstractClassBuilder.addMethod(convertModifier(abstractMethodBuilder));
								methodsToRemove.add(methodBuilder);
								break;
							}
						}
					}
				}

				templateClassBuilder.getDeclaredMethods().removeAll(methodsToRemove);

				for (JavaType innerTypeName : destType.getWatchedInnerTypes()) {
					for (ClassOrInterfaceTypeDetailsBuilder innerType : templateClassBuilder.getDeclaredInnerTypes()) {
						if (innerType.getName().getFullyQualifiedTypeName().equals(innerTypeName.getFullyQualifiedTypeName())) {
							ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractClassBuilder.getDeclaredByMetadataId(), innerType.build());
							builder.setName(new JavaType(innerType.getName().getSimpleTypeName() + "_Roo_Gwt", 0, DataType.TYPE, null, innerType.getName().getParameters()));

							templateClassBuilder.getDeclaredInnerTypes().remove(innerType);
							if (innerType.getPhysicalTypeCategory().equals(PhysicalTypeCategory.INTERFACE)) {
								ClassOrInterfaceTypeDetailsBuilder innerTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(innerType.build());
								abstractClassBuilder.addInnerType(builder);
								templateClassBuilder.getDeclaredInnerTypes().remove(innerType);
								innerTypeBuilder.getDeclaredMethods().clear();
								innerTypeBuilder.getDeclaredInnerTypes().clear();
								innerTypeBuilder.getExtendsTypes().clear();
								innerTypeBuilder.getExtendsTypes().add(new JavaType(builder.getName().getSimpleTypeName(), 0, DataType.TYPE, null, Collections.singletonList(new JavaType("V", 0, DataType.VARIABLE, null, new ArrayList<JavaType>()))));
								templateClassBuilder.getDeclaredInnerTypes().add(innerTypeBuilder);
							}
							break;
						}
					}
				}

				abstractClassBuilder.setImplementsTypes(templateClass.getImplementsTypes());
				templateClassBuilder.getImplementsTypes().clear();

				templateClassBuilder.getExtendsTypes().clear();
				templateClassBuilder.getExtendsTypes().add(abstractClassBuilder.getName());

				types.add(abstractClassBuilder.build());
			}

			types.add(templateClassBuilder.build());

			return types;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
