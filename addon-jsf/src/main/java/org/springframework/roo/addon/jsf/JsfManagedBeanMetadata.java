package org.springframework.roo.addon.jsf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfManagedBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = JsfManagedBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private JavaType entityType;
	private String plural;
	private List<MethodMetadata> locatedAccessors;
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata mergeMethod;
	private MethodMetadata removeMethod;
	private MethodMetadata findAllMethod;

	public JsfManagedBeanMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, JsfAnnotationValues annotationValues, MemberDetails memberDetails, String plural, List<MethodMetadata> locatedAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.isTrue(StringUtils.hasText(plural), "Plural required");
		
		if (!isValid()) {
			return;
		}
		
		entityType = annotationValues.getEntity();
		this.plural = plural;
		this.locatedAccessors = locatedAccessors;

		identifierAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		persistMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.PERSIST_METHOD);
		mergeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.MERGE_METHOD);
		removeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.REMOVE_METHOD);
		findAllMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_ALL_METHOD);
		if (identifierAccessorMethod == null || persistMethod == null || mergeMethod == null || removeMethod == null || findAllMethod == null) {
			return;
		}

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation());

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getSessionScopedAnnotation());

		// Add fields
		builder.addField(getEntityField());
		builder.addField(getAllEntitiesField());
		builder.addField(getTableVisibleField());

		// Add methods
		builder.addMethod(getCreateEntityMethod());
		builder.addMethod(getEntityAccessorMethod());
		builder.addMethod(getEntityMutatorMethod());
		builder.addMethod(getGetAllEntitiesMethod());
		builder.addMethod(getFindAllEntitiesMethod());
		builder.addMethod(getPersistMethod());
		builder.addMethod(getDeleteMethod());
		builder.addMethod(getResetMethod());
		builder.addMethod(getTableVisibleAccessorMethod());
		builder.addMethod(getTableVisibleMutatorMethod());
		builder.addInnerType(getConverterInnerType());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private AnnotationMetadata getManagedBeanAnnotation() {
		return getTypeAnnotation(new JavaType("javax.faces.bean.ManagedBean"));
	}

	private AnnotationMetadata getSessionScopedAnnotation() {
		return getTypeAnnotation(new JavaType("javax.faces.bean.SessionScoped"));
	}
	
	private AnnotationMetadata getTypeAnnotation(JavaType annotationType) {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, annotationType) != null) {
			return null;
		}
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(annotationType);
		return annotationBuilder.build();
	}

	private FieldMetadata getEntityField() {
		JavaSymbolName fieldName = new JavaSymbolName(StringUtils.uncapitalize(entityType.getSimpleTypeName()));
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, entityType);
		return fieldBuilder.build();
	}

	private FieldMetadata getAllEntitiesField() {
		JavaSymbolName fieldName = new JavaSymbolName("all" + plural);
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, getEntityListType());
		return fieldBuilder.build();
	}

	private JavaType getEntityListType() {
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(entityType);
		return new JavaType("java.util.List", 0, DataType.TYPE, null, paramTypes);
	}
	
	private FieldMetadata getTableVisibleField() {
		JavaSymbolName fieldName = new JavaSymbolName("tableVisible");
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, JavaType.BOOLEAN_PRIMITIVE, Boolean.FALSE.toString());
		return fieldBuilder.build();
	}

	private MethodMetadata getCreateEntityMethod() {
		JavaSymbolName methodName = new JavaSymbolName("create" + entityType.getSimpleTypeName());
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(StringUtils.uncapitalize(entityType.getSimpleTypeName()) + " = new " + entityType.getSimpleTypeName() + "();");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + "\";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getEntityAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("get" + entityType.getSimpleTypeName());
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getEntityMutatorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("set" + entityType.getSimpleTypeName());
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(entityType);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		String fieldName = StringUtils.uncapitalize(entityType.getSimpleTypeName());
		parameterNames.add(new JavaSymbolName(fieldName));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getGetAllEntitiesMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getAll" + plural);
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		JavaSymbolName fieldName = new JavaSymbolName("all" + plural);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(fieldName + " = " + entityType.getSimpleTypeName() + "." + findAllMethod.getMethodName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + fieldName + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, getEntityListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getFindAllEntitiesMethod() {
		JavaSymbolName methodName = new JavaSymbolName("findAll" + plural);
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		JavaSymbolName fieldName = new JavaSymbolName("all" + plural);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName + " = " + entityType.getSimpleTypeName() + "." + findAllMethod.getMethodName() + "();");
		bodyBuilder.appendFormalLine("tableVisible = !" + fieldName + ".isEmpty();");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + "\";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getPersistMethod() {
		JavaSymbolName methodName = new JavaSymbolName("persist");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		String fieldName = StringUtils.uncapitalize(entityType.getSimpleTypeName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + "." +  identifierAccessorMethod.getMethodName().getSymbolName() + "() != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(fieldName + "." + mergeMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(fieldName + "." + persistMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return " + findAllMethod.getMethodName() + "();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getDeleteMethod() {
		JavaSymbolName methodName = new JavaSymbolName("delete");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		String fieldName = StringUtils.uncapitalize(entityType.getSimpleTypeName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName + "." + removeMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return " + findAllMethod.getMethodName() + "();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getResetMethod() {
		JavaSymbolName methodName = new JavaSymbolName("reset");
		MethodMetadata method = methodExists(methodName,  new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(StringUtils.uncapitalize(entityType.getSimpleTypeName()) + " = null;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getTableVisibleAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("isTableVisible");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return tableVisible;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.BOOLEAN_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getTableVisibleMutatorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("setTableVisible");
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(JavaType.BOOLEAN_PRIMITIVE);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("tableVisible"));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this.tableVisible = tableVisible;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private ClassOrInterfaceTypeDetails getConverterInnerType() {
		JavaType innerType = new JavaType(entityType.getSimpleTypeName() + "Converter");
		if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, innerType) != null) {
			return null;
		}

		if (locatedAccessors.isEmpty()) {
			return null;
		}
		
		JavaType uiComponent = new JavaType("javax.faces.component.UIComponent");
		JavaType facesContext = new JavaType("javax.faces.context.FacesContext");
		JavaType converter = new JavaType("javax.faces.convert.Converter");

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(uiComponent);
		imports.addImport(converter);
		imports.addImport(facesContext);

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(facesContext);
		paramTypes.add(uiComponent);

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("context"));
		parameterNames.add(new JavaSymbolName("component"));
		parameterNames.add(new JavaSymbolName("value"));

		String typeName = StringUtils.uncapitalize(entityType.getSimpleTypeName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		// Create getAsObject method
		List<JavaType> getAsObjectParameterTypes = new ArrayList<JavaType>(paramTypes);
		getAsObjectParameterTypes.add(JavaType.STRING_OBJECT);
		bodyBuilder.indent();

		bodyBuilder.appendFormalLine(getEntityListType().getNameIncludingTypeParameters(false, imports) + " " + StringUtils.uncapitalize(plural) + " = " + entityType.getSimpleTypeName() + "." + findAllMethod.getMethodName() + "();");
		bodyBuilder.appendFormalLine("for (" + entityType.getSimpleTypeName() + " " + typeName + " : " + StringUtils.uncapitalize(plural) + ") {");

		StringBuilder sb = new StringBuilder("String ").append(typeName).append("Str = new StringBuilder()");
		int n = locatedAccessors.size();
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(").append(typeName).append(".").append(locatedAccessors.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(sb.toString());

		bodyBuilder.appendFormalLine("if (" + typeName +"Str.equals(value)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return " + typeName + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return null;");
		bodyBuilder.indentRemove();
		
		MethodMetadataBuilder getAsObjectMethod = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("getAsObject"), new JavaType("java.lang.Object"), AnnotatedJavaType.convertFromJavaTypes(getAsObjectParameterTypes), parameterNames, bodyBuilder);

		// Create getAsString method
		List<JavaType> getAsStringParameterTypes = new ArrayList<JavaType>(paramTypes);
		getAsStringParameterTypes.add(new JavaType("java.lang.Object"));
		bodyBuilder = new InvocableMemberBodyBuilder();
		
		sb = new StringBuilder("return new StringBuilder()");
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(((").append(entityType.getSimpleTypeName()).append(") value).").append(locatedAccessors.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(sb.toString());
		bodyBuilder.indentRemove();
		
		MethodMetadataBuilder getAsStringMethod = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("getAsString"), JavaType.STRING_OBJECT, AnnotatedJavaType.convertFromJavaTypes(getAsStringParameterTypes), parameterNames, bodyBuilder);

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, innerType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addImplementsType(converter);
		typeDetailsBuilder.addMethod(getAsObjectMethod);
		typeDetailsBuilder.addMethod(getAsStringMethod);

		return typeDetailsBuilder.build();
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName, List<JavaType> paramTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, paramTypes);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
