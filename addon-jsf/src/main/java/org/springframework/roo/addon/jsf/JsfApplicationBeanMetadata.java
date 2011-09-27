package org.springframework.roo.addon.jsf;

import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_CREATE_DIALOG;
import static org.springframework.roo.addon.jsf.JsfJavaType.DISPLAY_LIST;
import static org.springframework.roo.addon.jsf.JsfJavaType.EL_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.EXPRESSION_FACTORY;
import static org.springframework.roo.addon.jsf.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.JsfJavaType.MANAGED_BEAN;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_DEFAULT_MENU_MODEL;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MENU_ITEM;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_MENU_MODEL;
import static org.springframework.roo.addon.jsf.JsfJavaType.PRIMEFACES_SUB_MENU;
import static org.springframework.roo.addon.jsf.JsfJavaType.REQUEST_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.SESSION_SCOPED;
import static org.springframework.roo.addon.jsf.JsfJavaType.VIEW_SCOPED;
import static org.springframework.roo.model.JdkJavaType.POST_CONSTRUCT;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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
 * Metadata for {@link RooJsfApplicationBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfApplicationBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = JsfApplicationBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaSymbolName MENU_MODEL = new JavaSymbolName("menuModel");
	
	// Fields
	private Set<ClassOrInterfaceTypeDetails> managedBeans;

	public JsfApplicationBeanMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Set<ClassOrInterfaceTypeDetails> managedBeans, String projectName) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(managedBeans, "Managed beans required");
		Assert.isTrue(StringUtils.hasText(projectName), "Project name required");

		if (!isValid()) {
			return;
		}

		this.managedBeans = managedBeans;

		// Add @ManagedBean annotation if required
		builder.addAnnotation(getManagedBeanAnnotation());

		// Add @SessionScoped annotation if required
		builder.addAnnotation(getScopeAnnotation());

		// Add menu model field
		builder.addField(getField(MENU_MODEL, PRIMEFACES_MENU_MODEL));

		// Add init() method
		builder.addMethod(getInitMethod());

		// Add model field accessor method
		builder.addMethod(getAccessorMethod(MENU_MODEL, PRIMEFACES_MENU_MODEL));

		// Add application name accessor method
		builder.addMethod(getMethod(Modifier.PUBLIC, new JavaSymbolName("getAppName"), JavaType.STRING, null, null, InvocableMemberBodyBuilder.getInstance().appendFormalLine("return \"" + StringUtils.capitalize(projectName) + "\";")));

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	private AnnotationMetadata getManagedBeanAnnotation() {
		return getTypeAnnotation(MANAGED_BEAN);
	}

	private AnnotationMetadata getScopeAnnotation() {
		if (hasScopeAnnotation()) { 
			return null;
		}
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(SESSION_SCOPED);
		return annotationBuilder.build();
	}
	
	private boolean hasScopeAnnotation() {
		return (governorTypeDetails.getAnnotation(SESSION_SCOPED) != null 
			|| governorTypeDetails.getAnnotation(VIEW_SCOPED) != null 
			|| governorTypeDetails.getAnnotation(REQUEST_SCOPED) != null);
	}

	private MethodMetadata getInitMethod() {
		JavaSymbolName methodName = new JavaSymbolName("init");
		MethodMetadata method = getGovernorMethod(methodName);
		if (method != null) return method;

		ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(EL_CONTEXT);
		imports.addImport(EXPRESSION_FACTORY);
		imports.addImport(FACES_CONTEXT);
		imports.addImport(PRIMEFACES_MENU_ITEM);
		imports.addImport(PRIMEFACES_SUB_MENU);
		imports.addImport(PRIMEFACES_DEFAULT_MENU_MODEL);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		bodyBuilder.appendFormalLine("FacesContext facesContext = FacesContext.getCurrentInstance();");
		bodyBuilder.appendFormalLine("ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();");
		bodyBuilder.appendFormalLine("ELContext elContext = facesContext.getELContext();");
		bodyBuilder.appendFormalLine("");

		bodyBuilder.appendFormalLine("menuModel = new DefaultMenuModel();");
		bodyBuilder.appendFormalLine("Submenu submenu;");
		bodyBuilder.appendFormalLine("MenuItem item;");
		
		for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(managedBean.getAnnotations(), ROO_JSF_MANAGED_BEAN);
			if (annotation ==  null) {
				continue;
			}
			
			AnnotationAttributeValue<?> includeOnMenuValue = annotation.getAttribute(new JavaSymbolName("includeOnMenu"));
			if (includeOnMenuValue != null && !((Boolean) includeOnMenuValue.getValue()).booleanValue()) {
				continue;
			}

			AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("entity"));
			JavaType entity = (JavaType) value.getValue();
			String entityLabel = entity.getSimpleTypeName().length() > 26 ? entity.getSimpleTypeName().substring(0, 23) + "..." : entity.getSimpleTypeName();
			String beanName = StringUtils.uncapitalize(managedBean.getName().getSimpleTypeName());

			bodyBuilder.appendFormalLine("");
			bodyBuilder.appendFormalLine("submenu = new Submenu();");
			bodyBuilder.appendFormalLine("submenu.setId(\"" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "Submenu\");");
			bodyBuilder.appendFormalLine("submenu.setLabel(\"" + entityLabel + "\");");
			// bodyBuilder.appendFormalLine("submenu.setIcon(\"ui-icon ui-icon-document\");");

			bodyBuilder.appendFormalLine("item = new MenuItem();");
			bodyBuilder.appendFormalLine("item.setId(\"create" + entity.getSimpleTypeName() + "MenuItem\");");
			bodyBuilder.appendFormalLine("item.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{messages.global_menu_create}\", String.class));");
			bodyBuilder.appendFormalLine("item.setActionExpression(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + "." + DISPLAY_CREATE_DIALOG + "}\", String.class, new Class[0]));");
			bodyBuilder.appendFormalLine("item.setAjax(false);");
			bodyBuilder.appendFormalLine("item.setAsync(false);");
			bodyBuilder.appendFormalLine("submenu.getChildren().add(item);");

			bodyBuilder.appendFormalLine("item = new MenuItem();");
			bodyBuilder.appendFormalLine("item.setId(\"list" + entity.getSimpleTypeName() + "MenuItem\");");
			bodyBuilder.appendFormalLine("item.setValueExpression(\"value\", expressionFactory.createValueExpression(elContext, \"#{messages.global_menu_list}\", String.class));");
			bodyBuilder.appendFormalLine("item.setActionExpression(expressionFactory.createMethodExpression(elContext, \"#{" + beanName + "." + DISPLAY_LIST + "}\", String.class, new Class[0]));");
			bodyBuilder.appendFormalLine("item.setAjax(false);");
			bodyBuilder.appendFormalLine("item.setAsync(false);");
			bodyBuilder.appendFormalLine("submenu.getChildren().add(item);");

			bodyBuilder.appendFormalLine("menuModel.addSubmenu(submenu);");
		}

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(POST_CONSTRUCT));
		return methodBuilder.build();
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

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
