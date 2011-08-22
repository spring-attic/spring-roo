package org.springframework.roo.addon.jsf;

import static org.springframework.roo.addon.jsf.JsfUtils.CONVERTER;
import static org.springframework.roo.addon.jsf.JsfUtils.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfUtils.HTML_OUTPUT_TEXT;
import static org.springframework.roo.addon.jsf.JsfUtils.HTML_PANEL_GRID;
import static org.springframework.roo.addon.jsf.JsfUtils.PRIMEFACES_CALENDAR;
import static org.springframework.roo.addon.jsf.JsfUtils.PRIMEFACES_CLOSE_EVENT;
import static org.springframework.roo.addon.jsf.JsfUtils.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfUtils.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfUtils.UI_COMPONENT;
import static org.springframework.roo.addon.jsf.JsfUtils.VIEW_SCOPED;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = JsfManagedBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	private static final String NEW_DIALOG_VISIBLE = "newDialogVisible";
	
	// Fields
	private JavaType entityType;
	private String plural;
	private Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors;
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata mergeMethod;
	private MethodMetadata removeMethod;
	private MethodMetadata findAllMethod;

	public JsfManagedBeanMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, JsfManagedBeanAnnotationValues annotationValues, MemberDetails memberDetails, String plural, Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.isTrue(StringUtils.hasText(plural), "Plural required");
		Assert.notNull(locatedFieldsAndAccessors, "Located fields and accessors map required");
		
		if (!isValid()) {
			return;
		}
		
		entityType = annotationValues.getEntity();
		this.plural = plural;
		this.locatedFieldsAndAccessors = locatedFieldsAndAccessors;

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
		builder.addAnnotation(getScopeAnnotation());

		// Add fields
		builder.addField(getNameField());
		builder.addField(getSelectedEntityField());
		builder.addField(getAllEntitiesField());
		builder.addField(getColumnsField());
		builder.addField(getEditPanelField());
		builder.addField(getBooleanField(new JavaSymbolName(NEW_DIALOG_VISIBLE)));

		// Add methods
		builder.addMethod(getInitMethod());
		builder.addMethod(getNameAccessorMethod());
		builder.addMethod(getColumnsAccessorMethod());
		builder.addMethod(getSelectedEntityAccessorMethod());
		builder.addMethod(getSelectedEntityMutatorMethod());
		builder.addMethod(getAllEntitiesAccessorMethod());
		builder.addMethod(getAllEntitiesMutatorMethod());
		builder.addMethod(getFindAllEntitiesMethod());
		builder.addMethod(getEditPanelAccessorMethod());
		builder.addMethod(getEditPanelMutatorMethod());
		builder.addMethod(getPopulatePanelMethod());
		builder.addMethod(getBooleanAccessorMethod(NEW_DIALOG_VISIBLE));
		builder.addMethod(getBooleanMutatorMethod(NEW_DIALOG_VISIBLE));
		builder.addMethod(getDisplayListMethod());
		builder.addMethod(getDisplayNewDialogMethod());
		builder.addMethod(getPersistMethod());
		builder.addMethod(getDeleteMethod());
		builder.addMethod(getResetMethod());
		builder.addMethod(getHandleDialogCloseMethod());
		builder.addInnerType(getConverterInnerType());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private AnnotationMetadata getManagedBeanAnnotation() {
		return getTypeAnnotation(new JavaType("javax.faces.bean.ManagedBean"));
	}

	private AnnotationMetadata getScopeAnnotation() {
		if (hasScopeAnnotation()) { 
			return null;
		}
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(SESSION_SCOPED);
		return annotationBuilder.build();
	}
	
	private boolean hasScopeAnnotation() {
		return (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, SESSION_SCOPED) != null 
			|| MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, VIEW_SCOPED) != null 
			|| MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, REQUEST_SCOPED) != null);
	}
	
	private FieldMetadata getSelectedEntityField() {
		JavaSymbolName fieldName = new JavaSymbolName(getEntityName());
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, entityType);
		return fieldBuilder.build();
	}

	private FieldMetadata getAllEntitiesField() {
		JavaSymbolName fieldName = new JavaSymbolName("all" + plural);
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType("java.util.List"));

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, getEntityListType());
		return fieldBuilder.build();
	}

	private JavaType getEntityListType() {
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(entityType);
		return new JavaType("java.util.List", 0, DataType.TYPE, null, paramTypes);
	}
	
	private FieldMetadata getNameField() {
		JavaSymbolName fieldName = new JavaSymbolName("name");
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, JavaType.STRING_OBJECT, "\"" + plural + "\"");
		return fieldBuilder.build();
	}

	private FieldMetadata getColumnsField() {
		JavaSymbolName fieldName = new JavaSymbolName("columns");
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, getColumnsListType());
		return fieldBuilder.build();
	}

	private JavaType getColumnsListType() {
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.STRING_OBJECT);
		return new JavaType("java.util.List", 0, DataType.TYPE, null, paramTypes);
	}

	private FieldMetadata getEditPanelField() {
		JavaSymbolName fieldName = new JavaSymbolName("editPanel");
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, HTML_PANEL_GRID);
		return fieldBuilder.build();
	}
	
	private MethodMetadata getInitMethod() {
		JavaSymbolName methodName = new JavaSymbolName("init");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType("java.util.ArrayList"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("all" + plural + " = " + entityType.getSimpleTypeName() + ".findAll" + plural + "();");
		bodyBuilder.appendFormalLine("columns = new ArrayList<String>();");
		for (FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			bodyBuilder.appendFormalLine("columns.add(\"" + field.getFieldName().getSymbolName() + "\");");
		}
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("javax.annotation.PostConstruct")));
		return methodBuilder.build();
	}
	
	private MethodMetadata getColumnsAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getColumns");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return columns;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, getColumnsListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getSelectedEntityAccessorMethod() {
		String fieldName = getEntityName();
		JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(fieldName));
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(fieldName + " = new " + entityType.getSimpleTypeName() + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getSelectedEntityMutatorMethod() {
		String fieldName = getEntityName();
		JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(entityType);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName(fieldName));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getAllEntitiesAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getAll" + plural);
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		JavaSymbolName fieldName = new JavaSymbolName("all" + plural);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + fieldName + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, getEntityListType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getAllEntitiesMutatorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("setAll" + plural);
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(entityType);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return all" + plural + ";");

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
		bodyBuilder.appendFormalLine("return null;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getNameAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getName");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return name;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getEditPanelAccessorMethod() {
		String fieldName = "editPanel";
		JavaSymbolName methodName = new JavaSymbolName("get" + StringUtils.capitalize(fieldName));
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + fieldName + " == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(fieldName + " = populatePanel();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getEditPanelMutatorMethod() {
		String fieldName = "editPanel";
		JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(HTML_PANEL_GRID);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(HTML_PANEL_GRID);
		
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName(fieldName));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getPopulatePanelMethod() {
		JavaSymbolName methodName = new JavaSymbolName("populatePanel");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		JsfUtils.addCommonJsfFields(imports, bodyBuilder);
		imports.addImport(HTML_PANEL_GRID);
		imports.addImport(HTML_OUTPUT_TEXT);

		bodyBuilder.appendFormalLine("HtmlPanelGrid htmlPanelGrid = " + getComponentCreationStr("HtmlPanelGrid"));
		bodyBuilder.appendFormalLine("htmlPanelGrid.setId(\"editPanelGrid\");");
		bodyBuilder.appendFormalLine("");
		
		for (FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			JavaType fieldType = field.getFieldType();
			String fieldName = field.getFieldName().getSymbolName();
			String outputFieldVar = fieldName + "Output";
			String inputFieldVar = fieldName + "Input";
			
			bodyBuilder.appendFormalLine("HtmlOutputText " + outputFieldVar + " = " + getComponentCreationStr("HtmlOutputText"));
			bodyBuilder.appendFormalLine(outputFieldVar + ".setId(\"" + outputFieldVar + "\");");
			bodyBuilder.appendFormalLine(outputFieldVar + ".setValue(\"" + fieldName + "\");");
			bodyBuilder.appendFormalLine("htmlPanelGrid.getChildren().add(" + outputFieldVar + ");");
			bodyBuilder.appendFormalLine("");
			if (isDateField(fieldType)) {
				imports.addImport(PRIMEFACES_CALENDAR);
				imports.addImport(new JavaType("java.util.Date"));
				bodyBuilder.appendFormalLine("Calendar " + inputFieldVar + " = " + getComponentCreationStr("Calendar"));
				bodyBuilder.appendFormalLine(getValueExpressionStr(inputFieldVar, fieldName, Date.class));
				bodyBuilder.appendFormalLine(inputFieldVar + ".setNavigator(true);");
				bodyBuilder.appendFormalLine(inputFieldVar + ".setEffect(\"slideDown\");");
				bodyBuilder.appendFormalLine(inputFieldVar + ".setPattern(\"dd/MM/yyyy\");");
			} else {
				imports.addImport(new JavaType("org.primefaces.component.inputtext.InputText"));
				bodyBuilder.appendFormalLine("InputText " + inputFieldVar + " = " + getComponentCreationStr("InputText"));
				bodyBuilder.appendFormalLine(getValueExpressionStr(inputFieldVar, fieldName, String.class));
			}
			bodyBuilder.appendFormalLine(inputFieldVar + ".setId(\"" + inputFieldVar + "\");");
			bodyBuilder.appendFormalLine("htmlPanelGrid.getChildren().add(" + inputFieldVar + ");");	
			bodyBuilder.appendFormalLine("");
		}
		
		bodyBuilder.appendFormalLine("return htmlPanelGrid;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, HTML_PANEL_GRID, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayNewDialogMethod() {
		JavaSymbolName methodName = new JavaSymbolName("displayNewDialog");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(NEW_DIALOG_VISIBLE + " = true;");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + "\";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getDisplayListMethod() {
		JavaSymbolName methodName = new JavaSymbolName("displayList");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(NEW_DIALOG_VISIBLE + " = false;");
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + "\";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getPersistMethod() {
		JavaSymbolName methodName = new JavaSymbolName("persist");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		String fieldName = getEntityName();

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
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getDeleteMethod() {
		JavaSymbolName methodName = new JavaSymbolName("delete");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getEntityName() + "." + removeMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine("reset();");
		bodyBuilder.appendFormalLine("return findAll" + plural + "();");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getResetMethod() {
		JavaSymbolName methodName = new JavaSymbolName("reset");
		MethodMetadata method = methodExists(methodName,  new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getEntityName() + " = null;");
		bodyBuilder.appendFormalLine(NEW_DIALOG_VISIBLE + " = false;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getHandleDialogCloseMethod() {
		JavaSymbolName methodName = new JavaSymbolName("handleDialogClose");
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(PRIMEFACES_CLOSE_EVENT);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_CLOSE_EVENT);
		
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("event"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("reset();");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private String getEntityName() {
		return "selected" + entityType.getSimpleTypeName();
	}
	
	private FieldMetadata getBooleanField(JavaSymbolName fieldName) {
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, JavaType.BOOLEAN_PRIMITIVE, Boolean.FALSE.toString());
		return fieldBuilder.build();
	}

	private MethodMetadata getBooleanAccessorMethod(String fieldName) {
		JavaSymbolName methodName = new JavaSymbolName("is" + StringUtils.capitalize(fieldName));
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.BOOLEAN_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getBooleanMutatorMethod(String fieldName) {
		JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(fieldName));
		List<JavaType> parameterTypes = new ArrayList<JavaType>();
		parameterTypes.add(JavaType.BOOLEAN_PRIMITIVE);
		MethodMetadata method = methodExists(methodName, parameterTypes);
		if (method != null) return method;

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName(fieldName));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + fieldName + " = " + fieldName + ";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private ClassOrInterfaceTypeDetails getConverterInnerType() {
		String simpleTypeName = entityType.getSimpleTypeName();
		JavaType innerType = new JavaType(simpleTypeName + "Converter");
		if (MemberFindingUtils.getDeclaredInnerType(governorTypeDetails, innerType) != null) {
			return null;
		}

		List<MethodMetadata> converterMethods = getConverterMethods();
		if (converterMethods.isEmpty()) {
			return null;
		}
		
		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(UI_COMPONENT);
		imports.addImport(CONVERTER);
		imports.addImport(FACES_CONTEXT);

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(FACES_CONTEXT);
		paramTypes.add(UI_COMPONENT);

		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		parameterNames.add(new JavaSymbolName("context"));
		parameterNames.add(new JavaSymbolName("component"));
		parameterNames.add(new JavaSymbolName("value"));

		String typeName = StringUtils.uncapitalize(simpleTypeName);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		// Create getAsObject method
		List<JavaType> getAsObjectParameterTypes = new ArrayList<JavaType>(paramTypes);
		getAsObjectParameterTypes.add(JavaType.STRING_OBJECT);
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(getEntityListType().getNameIncludingTypeParameters(false, imports) + " " + StringUtils.uncapitalize(plural) + " = " + simpleTypeName + "." + findAllMethod.getMethodName() + "();");
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + typeName + " : " + StringUtils.uncapitalize(plural) + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(new StringBuilder("String ").append(typeName).append("Str = ").append(getBuilderString(converterMethods)).toString());
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
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(simpleTypeName + " " + typeName + " = (" + simpleTypeName + ") value;" );
		bodyBuilder.appendFormalLine(new StringBuilder("return ").append(getBuilderString(converterMethods)).toString());
		bodyBuilder.indentRemove();
		
		MethodMetadataBuilder getAsStringMethod = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("getAsString"), JavaType.STRING_OBJECT, AnnotatedJavaType.convertFromJavaTypes(getAsStringParameterTypes), parameterNames, bodyBuilder);

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, innerType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addImplementsType(CONVERTER);
		typeDetailsBuilder.addMethod(getAsObjectMethod);
		typeDetailsBuilder.addMethod(getAsStringMethod);

		return typeDetailsBuilder.build();
	}

	private MethodMetadata methodExists(JavaSymbolName methodName, List<JavaType> paramTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, paramTypes);
	}
	
	private List<MethodMetadata> getConverterMethods() {
		List<MethodMetadata> converterMethods = new LinkedList<MethodMetadata>();
		for (FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			if (field.getCustomData() != null && field.getCustomData().keySet().contains("converterField")) {
				converterMethods.add(locatedFieldsAndAccessors.get(field));
			}
		}
		return converterMethods;
	}

	private String getBuilderString(List<MethodMetadata> converterMethods) {
		StringBuilder sb = new StringBuilder("new StringBuilder()");
		for (int i = 0; i < converterMethods.size(); i++) {
			if (i > 0) {
				sb.append(".append(\" \")");
			}
			sb.append(".append(").append(StringUtils.uncapitalize(entityType.getSimpleTypeName())).append(".").append(converterMethods.get(i).getMethodName().getSymbolName()).append("())");
		}
		sb.append(".toString();");
		return sb.toString();
	}
	
	private String getComponentCreationStr(String componentName) {
		return new StringBuilder().append("(").append(componentName).append(") facesContext.getApplication().createComponent(").append(componentName).append(".COMPONENT_TYPE);").toString();
	}
	
	private String getValueExpressionStr(String inputFieldVar, String fieldName, Class<?> clazz) {
		return inputFieldVar + ".setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{" + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + "Bean.selected" + entityType.getSimpleTypeName() + "." + fieldName + "}\", " + clazz.getSimpleName() + ".class));";
	}

	private boolean isDateField(JavaType fieldType) {
		return fieldType.equals(new JavaType("java.util.Date")) || fieldType.equals(new JavaType("java.util.Calendar")) || fieldType.equals(new JavaType("java.util.GregorianCalendar"));
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
