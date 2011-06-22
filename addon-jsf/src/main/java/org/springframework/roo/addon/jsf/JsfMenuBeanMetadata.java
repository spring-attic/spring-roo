package org.springframework.roo.addon.jsf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jvnet.inflector.Noun;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfMenuBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfMenuBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = JsfMenuBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType PRIMEFACES_MENU_MODEL = new JavaType("org.primefaces.model.MenuModel");
	private Set<ClassOrInterfaceTypeDetails> managedBeans;
	private String projectName;

	public JsfMenuBeanMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Set<ClassOrInterfaceTypeDetails> managedBeans, String projectName) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(managedBeans, "Managed beans required");
		Assert.isTrue(StringUtils.hasText(projectName), "Project name required");

		if (!isValid()) {
			return;
		}

		this.managedBeans = managedBeans;
		this.projectName = projectName;

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation());

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getSessionScopedAnnotation());

		if (!managedBeans.isEmpty()) {
			// Add menu model field
			builder.addField(getMenuModelField());

			// Add constructor
			builder.addConstructor(getConstructor());

			// Add model field accessor method
			builder.addMethod(getModelAccessorMethod());
		}

		// Add application name accessor method
		builder.addMethod(getApplicationAccessorMethod());

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
	
	private FieldMetadata getMenuModelField() {
		JavaSymbolName fieldName = new JavaSymbolName("menuModel");
		FieldMetadata field = MemberFindingUtils.getField(governorTypeDetails, fieldName);
		if (field != null) return field;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_MENU_MODEL);

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new ArrayList<AnnotationMetadataBuilder>(), fieldName, PRIMEFACES_MENU_MODEL);
		return fieldBuilder.build();
	}
	
	private ConstructorMetadata getConstructor() {
		ConstructorMetadata constructor = MemberFindingUtils.getDeclaredConstructor(governorTypeDetails, new ArrayList<JavaType>());
		if (constructor != null) return constructor;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(new JavaType("javax.el.ELContext"));
		imports.addImport(new JavaType("javax.el.ExpressionFactory"));
		imports.addImport(new JavaType("javax.faces.context.FacesContext"));
		imports.addImport(new JavaType("org.primefaces.component.menuitem.MenuItem"));
		imports.addImport(new JavaType("org.primefaces.component.submenu.Submenu"));
		imports.addImport(new JavaType("org.primefaces.model.DefaultMenuModel"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();");
		bodyBuilder.appendFormalLine("ELContext elContext = facesContext.getELContext();");
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine("menuModel = new DefaultMenuModel();");
		bodyBuilder.appendFormalLine("Submenu submenu;");
		bodyBuilder.appendFormalLine("MenuItem item;");
		
		for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(managedBean.getAnnotations(), new JavaType(RooJsfManagedBean.class.getName()));
			if (annotation ==  null) {
				continue;
			}
			
			AnnotationAttributeValue<?> includeOnMenuValue = annotation.getAttribute(new JavaSymbolName("includeOnMenu"));
			if (includeOnMenuValue != null && !((Boolean) includeOnMenuValue.getValue()).booleanValue()) {
				continue;
			}

			AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("entity"));
			JavaType entity = (JavaType) value.getValue();
			String plural = getInflectorPlural(entity.getSimpleTypeName());

			bodyBuilder.appendFormalLine("");
			bodyBuilder.appendFormalLine("submenu = new Submenu();");
			bodyBuilder.appendFormalLine("submenu.setLabel(\"" + entity.getSimpleTypeName() + "\");");

			bodyBuilder.appendFormalLine("item = new MenuItem();");
			bodyBuilder.appendFormalLine("item.setValue(\"List all " + plural + "\");");
			bodyBuilder.appendFormalLine("item.setActionExpression(expressionFactory.createMethodExpression(elContext, \"#{" + StringUtils.uncapitalize(managedBean.getName().getSimpleTypeName()) + ".findAll" + plural + "}\", String.class, new Class[0]));");
			bodyBuilder.appendFormalLine("item.setAjax(false);");
			bodyBuilder.appendFormalLine("item.setAsync(false);");
			bodyBuilder.appendFormalLine("item.setHelpText(\"List all " + entity.getSimpleTypeName() + " domain objects\");");
			bodyBuilder.appendFormalLine("submenu.getChildren().add(item);");

			bodyBuilder.appendFormalLine("menuModel.addSubmenu(submenu);");
		}

		ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
		constructorBuilder.setModifier(Modifier.PUBLIC);
		constructorBuilder.setBodyBuilder(bodyBuilder);
		return constructorBuilder.build();
	}

	private MethodMetadata getModelAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getMenuModel");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(PRIMEFACES_MENU_MODEL);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return menuModel;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, PRIMEFACES_MENU_MODEL, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata getApplicationAccessorMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getAppName");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return \"" + StringUtils.capitalize(projectName) + "\";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata methodExists(JavaSymbolName methodName, List<JavaType> paramTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, paramTypes);
	}

	private String getInflectorPlural(String term) {
		try {
			return Noun.pluralOf(term, Locale.ENGLISH);
		} catch (RuntimeException e) {
			// Inflector failed (see for example ROO-305), so don't pluralize it
			return term;
		}
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
