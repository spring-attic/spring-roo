package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.*;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for GWT.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
public class GwtMetadata extends AbstractMetadataItem {
	private static final String PROVIDES_TYPE_STRING = GwtMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private FileManager fileManager;
	private MetadataService metadataService;
	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private MethodMetadata findAllMethod;
	private MethodMetadata findMethod;
	private MethodMetadata countMethod;
	private MethodMetadata findEntriesMethod;

	private MirrorTypeNamingStrategy mirrorTypeNamingStrategy;
	private ProjectMetadata projectMetadata;
	private ClassOrInterfaceTypeDetails governorTypeDetails;
	private Path mirrorTypePath;
	private ClassOrInterfaceTypeDetails request;

	private ClassOrInterfaceTypeDetails proxy;
	private List<MethodMetadata> proxyDeclaredMethods;
	private JavaSymbolName idPropertyName;
	private JavaSymbolName versionPropertyName;

	private ClassOrInterfaceTypeDetails details;
	private ClassOrInterfaceTypeDetails listView;

	public GwtMetadata(String identifier, MirrorTypeNamingStrategy mirrorTypeNamingStrategy, ProjectMetadata projectMetadata, ClassOrInterfaceTypeDetails governorTypeDetails, Path mirrorTypePath, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FileManager fileManager, MetadataService metadataService) {
		super(identifier);
		this.mirrorTypeNamingStrategy = mirrorTypeNamingStrategy;
		this.projectMetadata = projectMetadata;
		this.governorTypeDetails = governorTypeDetails;
		this.mirrorTypePath = mirrorTypePath;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.fileManager = fileManager;
		this.metadataService = metadataService;

		// We know GwtMetadataProvider already took care of all the necessary checks. So we can just re-create fresh representations of the types we're responsible for
		resolveEntityInformation();
		buildProxy();
		buildActivitiesMapper();

		buildEditActivityWrapper();
		buildDetailsActivity();
		buildListActivity();
		buildMobileListView();
		buildListView();
		buildListViewUiXml();
		buildDetailsView();
		buildDetailsViewUiXml();
		buildMobileDetailsView();
		buildMobileDetailsViewUiXml();
		buildEditView();
		buildEditViewUiXml();
		buildMobileEditView();
		buildMobileEditViewUiXml();
		buildEditRenderer();
                buildSetEditor();
                buildSetEditorUiXml();
		buildRequest();
	}

	public List<ClassOrInterfaceTypeDetails> getAllTypes() {
		List<ClassOrInterfaceTypeDetails> result = new ArrayList<ClassOrInterfaceTypeDetails>();
		result.add(proxy);
		result.add(request);
		return result;
	}

	private void resolveEntityInformation() {
		if (entityMetadata != null && entityMetadata.isValid()) {
			// Lookup special fields
		  FieldMetadata versionField = entityMetadata.getVersionField();
		  FieldMetadata idField = entityMetadata.getIdentifierField();
		  Assert.notNull(versionField, "Version unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
		  Assert.notNull(idField, "Id unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
			versionPropertyName = versionField.getFieldName();
			idPropertyName = idField.getFieldName();

			// Lookup the "find all" method and store it
			findAllMethod = entityMetadata.getFindAllMethod();
			findMethod = entityMetadata.getFindMethod();
			findEntriesMethod = entityMetadata.getFindEntriesMethod();
			countMethod = entityMetadata.getCountMethod();
      Assert.notNull(findAllMethod, "Find all method unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
      Assert.isTrue("id".equals(idPropertyName.getSymbolName()), "Id property must be named \"id\" (found \""+ idPropertyName + "\") for " + governorTypeDetails.getName() + " - required for GWT support");
      Assert.isTrue("version".equals(versionPropertyName.getSymbolName()), "Version property must be named \"version\" (found \""+ versionPropertyName + "\") for " + governorTypeDetails.getName() + " - required for GWT support");
      Assert.notNull(findAllMethod, "Find all method unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
		}
	}

	private void buildActivitiesMapper() {
		try {
			MirrorType type = MirrorType.ACTIVITIES_MAPPER;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
			addReference(dataDictionary, SharedType.SCAFFOLD_APP);
			addReference(dataDictionary, MirrorType.DETAIL_ACTIVITY);
			addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);
      addReference(dataDictionary, MirrorType.LIST_VIEW);
      addReference(dataDictionary, MirrorType.DETAILS_VIEW);
      addReference(dataDictionary, MirrorType.MOBILE_DETAILS_VIEW);
      addReference(dataDictionary, MirrorType.EDIT_VIEW);
      addReference(dataDictionary, MirrorType.MOBILE_EDIT_VIEW);
      addReference(dataDictionary, MirrorType.REQUEST);
			writeWithTemplate(type, dataDictionary, type.getTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildProxy() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.PROXY);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();
		// @ProxyFor(Employee.class)
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ProxyFor")));
		List<ConstructorMetadataBuilder> constructors = new ArrayList<ConstructorMetadataBuilder>();
		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// extends Proxy
		extendsTypes.add(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"));

		/*
		 * Decide which fields we'll be mapping. Remember the natural ordering for
		 * processing, but order proxy getters alphabetically by name. 
		 */
		List<JavaSymbolName> propertyNames = new ArrayList<JavaSymbolName>();
		SortedMap<JavaSymbolName, JavaType> propToGwtSideType = new TreeMap<JavaSymbolName, JavaType>();
		if (beanInfoMetadata != null) {
			for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
				JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor).getSymbolName()));
				propertyNames.add(propertyName);
                FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);
                
                boolean serverOnly = false;
                for (AnnotationMetadata annotation : field.getAnnotations()) {
                    if (annotation.getAnnotationType().getSimpleTypeName().equals("RooServerOnly")) {
                        serverOnly = true;
                        break;
                    }
                }
                
                if (!serverOnly) {                    
                    JavaType returnType = accessor.getReturnType();
                    checkPrimitive(returnType);
                    
                    PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(returnType, Path.SRC_MAIN_JAVA));

                    JavaType gwtSideType = getGwtSideLeafType(returnType, ptmd);

                    // Store in the maps
                    propToGwtSideType.put(propertyName, gwtSideType);
                }
			}
		}

		// Getter methods for EmployeeProxy
		Map<JavaSymbolName, JavaSymbolName> propertyToMethod = new HashMap<JavaSymbolName, JavaSymbolName>();
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaType methodReturnType = propToGwtSideType.get(propertyName);
			JavaSymbolName methodName = new JavaSymbolName("get" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
			List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();
			propertyToMethod.put(propertyName, methodName);
			methods.add(new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder()));
		}

		// Setter methods for EmployeeProxy
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaType methodReturnType = JavaType.VOID_PRIMITIVE;
			// propToGwtSideType.get(propertyName);
			JavaSymbolName methodName = new JavaSymbolName("set" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = Collections.<JavaType> singletonList(propToGwtSideType.get(propertyName));
			List<JavaSymbolName> methodParameterNames = Collections.<JavaSymbolName> singletonList(propertyName);
			methods.add(new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder()));
		}

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(destinationMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);
		typeDetailsBuilder.setDeclaredConstructors(constructors);
		typeDetailsBuilder.setDeclaredMethods(methods);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		typeDetailsBuilder.setImplementsTypes(implementsTypes);
		typeDetailsBuilder.setAnnotations(typeAnnotations);
		this.proxy = typeDetailsBuilder.build();

		/*
		 * The methods in the proxy will be sorted alphabetically, which makes sense
		 * for the Java type. However, we want to process them in the order the
		 * fields are declared, such that the first database field is the first
		 * field we add to the dataDictionary. This affects the order of the
		 * properties in the desktop client, as well as the primary/secondary
		 * properties in the mobile client.
		 */
		Map<JavaSymbolName, MethodMetadata> methodNameToMethod = new HashMap<JavaSymbolName, MethodMetadata>();
		for (MethodMetadata method : proxy.getDeclaredMethods()) {
			methodNameToMethod.put(method.getMethodName(), method);
		}
		proxyDeclaredMethods = new ArrayList<MethodMetadata>();
		for (JavaSymbolName propertyName : propertyNames) {
			JavaSymbolName methodName = propertyToMethod.get(propertyName);
			proxyDeclaredMethods.add(methodNameToMethod.get(methodName));
		}
	}
	
	private void checkPrimitive(JavaType returnType) {
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

        	throw new IllegalStateException("GWT does not currently support primitive types in an entity. Please change any '" + from + "' entity property types to 'java.lang." + to + "'.");
    	}
    }

  private JavaType getGwtSideLeafType(JavaType returnType, PhysicalTypeMetadata ptmd) {
    boolean isDomainObject = isDomainObject(returnType, ptmd);
    if (isDomainObject) {
      return getDestinationJavaType(returnType, MirrorType.PROXY);
    }
    if (returnType.isPrimitive()) {
      	if (returnType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
        	return JavaType.BOOLEAN_OBJECT;
      	}
      	if (returnType.equals(JavaType.INT_PRIMITIVE)) {
      		  return JavaType.INT_OBJECT;
      	}
      	if (returnType.equals(JavaType.BYTE_PRIMITIVE)) {
        	return JavaType.BYTE_OBJECT;
      	}
      	if (returnType.equals(JavaType.SHORT_PRIMITIVE)) {
        	return JavaType.SHORT_OBJECT;
      	}
      	if (returnType.equals(JavaType.FLOAT_PRIMITIVE)) {
      	  	return JavaType.FLOAT_OBJECT;
      	}
      	if (returnType.equals(JavaType.DOUBLE_PRIMITIVE)) {
      	  	return JavaType.DOUBLE_OBJECT;
      	}
      	if (returnType.equals(JavaType.CHAR_PRIMITIVE)) {
        	return JavaType.CHAR_OBJECT;
      	}
      	if (returnType.equals(JavaType.LONG_PRIMITIVE)) {
        	return JavaType.LONG_OBJECT;
      	}    	
    	return returnType;
    }
    
    if (isCollectionType(returnType)) {
      List<JavaType> args = returnType.getParameters();
      if (args != null && args.size() == 1) {
        JavaType elementType = args.get(0);
        if (isDomainObject(elementType, (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(elementType, Path.SRC_MAIN_JAVA)))) {
          return new JavaType(returnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(getDestinationJavaType(elementType, MirrorType.PROXY)));
        }
        else {
          return new JavaType(returnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(elementType));
        }

      }
      return returnType;

    }
    return returnType;
  }

  private boolean isDomainObject(JavaType returnType, PhysicalTypeMetadata ptmd) {
    boolean isEnum = ptmd != null 
      && ptmd.getPhysicalTypeDetails() != null 
      && ptmd.getPhysicalTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;

    boolean isDomainObject = !isEnum 
      && !isShared(returnType) 
      && !(isRequestFactoryPrimitive(returnType))
      && !(isCollectionType(returnType));

    return isDomainObject;
  }

  private boolean isCollectionType(JavaType returnType) {
    return returnType.equals(new JavaType("java.util.List"))
         || returnType.equals(new JavaType("java.util.Set"));
  }

  private boolean isRequestFactoryPrimitive(JavaType returnType) {
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

	private void addReference(TemplateDataDictionary dataDictionary, MirrorType type) {
		addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private void addReference(TemplateDataDictionary dataDictionary, SharedType type) {
		addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
		dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private void addImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
		dataDictionary.addSection("imports").setVariable("import", importDeclaration);
	}

	private void buildEditActivityWrapper() {
		try {
			MirrorType type = MirrorType.EDIT_ACTIVITY_WRAPPER;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
			addReference(dataDictionary, SharedType.IS_SCAFFOLD_MOBILE_ACTIVITY);
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildDetailsActivity() {
		try {
			MirrorType type = MirrorType.DETAIL_ACTIVITY;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
			addReference(dataDictionary, SharedType.IS_SCAFFOLD_MOBILE_ACTIVITY);
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildListActivity() {
		try {
			MirrorType type = MirrorType.LIST_ACTIVITY;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.SCAFFOLD_MOBILE_APP);
			addReference(dataDictionary, SharedType.IS_SCAFFOLD_MOBILE_ACTIVITY);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildMobileListView() {
		try {
			MirrorType type = MirrorType.MOBILE_LIST_VIEW;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.MOBILE_PROXY_LIST_VIEW);
			addReference(dataDictionary, SharedType.SCAFFOLD_MOBILE_APP);
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildListView() {
		try {
			writeWithTemplate(MirrorType.LIST_VIEW);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void writeWithTemplate(MirrorType destType) throws TemplateException {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, destType, destType.getTemplate());
	}

	private void writeWithTemplate(MirrorType destType, String templateFile) throws TemplateException {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, destType, templateFile);
	}

	private void writeWithTemplate(String destFile, MirrorType destType, String templateFile) throws TemplateException {
		writeWithTemplate(destFile, buildDataDictionary(destType), templateFile);
	}

	private void writeWithTemplate(MirrorType destType, TemplateDataDictionary dataDictionary, String templateFile) throws TemplateException {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, dataDictionary, templateFile);
	}

	private void writeWithTemplate(MirrorType destType, TemplateDataDictionary dataDictionary) throws TemplateException {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, dataDictionary, destType.getTemplate());
	}

	private void writeWithTemplate(String destFile, TemplateDataDictionary dataDictionary, String templateFile) throws TemplateException {
		TemplateLoader templateLoader = TemplateResourceLoader.create();
		Template template = templateLoader.getTemplate(templateFile);
		write(destFile, template.renderToString(dataDictionary), fileManager);
	}

	private boolean isShared(JavaType type) {
		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (ptmd != null) {
			return ptmd.getPhysicalLocationCanonicalPath().startsWith(GwtPath.SHARED.canonicalFileSystemPath(projectMetadata));
		}
		return false;
	}

	private TemplateDataDictionary buildDataDictionary(MirrorType destType) {
		JavaType javaType = getDestinationJavaType(destType);
		JavaType proxyType = getDestinationJavaType(MirrorType.PROXY);

		TemplateDataDictionary dataDictionary = TemplateDictionary.create();
		addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());
		dataDictionary.setVariable("className", javaType.getSimpleTypeName());
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		dataDictionary.setVariable("placePackage", GwtPath.PLACE.packageName(projectMetadata));
		dataDictionary.setVariable("scaffoldUiPackage", GwtPath.GWT_SCAFFOLD_UI.packageName(projectMetadata));
                dataDictionary.setVariable("uiPackage", GwtPath.GWT_UI.packageName(projectMetadata));
		dataDictionary.setVariable("name", governorTypeDetails.getName().getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
                dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
                dataDictionary.setVariable("proxyRenderer", GwtProxyProperty.getProxyRendererType(projectMetadata, proxyType));
		String proxyFields = null;
		GwtProxyProperty primaryProp = null;
		GwtProxyProperty secondaryProp = null;
		GwtProxyProperty dateProp = null;
		Set<String> importSet = new HashSet<String>();
		for (MethodMetadata method : proxyDeclaredMethods) {
			if (!GwtProxyProperty.isAccessor(method)) {
				continue;
			}
                      
                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(method.getReturnType(), Path.SRC_MAIN_JAVA));
			GwtProxyProperty property = new GwtProxyProperty(projectMetadata, method, ptmd);

			// Determine if this is the primary property.
			if (primaryProp == null) {
				// Choose the first available field.
				primaryProp = property;
			} else if (property.isString() && !primaryProp.isString()) {
				// Favor String properties over other types.
				secondaryProp = primaryProp;
				primaryProp = property;
			} else if (secondaryProp == null) {
				// Choose the next available property.
				secondaryProp = property;
			} else if (property.isString() && !secondaryProp.isString()) {
				// Favor String properties over other types.
				secondaryProp = property;
			}

			// Determine if this is the first date property.
			if (dateProp == null && property.isDate()) {
				dateProp = property;
			}

			if (property.isProxy()) {
				if (proxyFields != null) {
					proxyFields += ", ";
				} else {
					proxyFields = "";
				}
				proxyFields += "\"" + property.getName() + "\"";
			}

			dataDictionary.addSection("fields").setVariable("field", property.getName());
			if (!isReadOnly(property.getName())) dataDictionary.addSection("editViewProps").setVariable("prop", property.forEditView());

			TemplateDataDictionary propertiesSection = dataDictionary.addSection("properties");
			propertiesSection.setVariable("prop", property.getName());
			propertiesSection.setVariable("propGetter", property.getGetter());
			propertiesSection.setVariable("propType", property.getType());
			propertiesSection.setVariable("propFormatter", property.getFormatter());
			propertiesSection.setVariable("propRenderer", property.getRenderer());
			propertiesSection.setVariable("propReadable", property.getReadableName());

			if (!isReadOnly(property.getName())) {
				TemplateDataDictionary editableSection = dataDictionary.addSection("editableProperties");
				editableSection.setVariable("prop", property.getName());
				editableSection.setVariable("propGetter", property.getGetter());
				editableSection.setVariable("propType", property.getType());
				editableSection.setVariable("propFormatter", property.getFormatter());
				editableSection.setVariable("propRenderer", property.getRenderer());
				editableSection.setVariable("propBinder", property.getBinder());
				editableSection.setVariable("propReadable", property.getReadableName());	       
			}

			dataDictionary.setVariable("proxyRendererType", MirrorType.EDIT_RENDERER.getPath().packageName(projectMetadata) + "." + proxy.getName().getSimpleTypeName() + "Renderer");

			if (property.isProxy() || property.isEnum() || property.isCollectionOfProxy()) {
				TemplateDataDictionary section = dataDictionary.addSection(property.isEnum() ? "setEnumValuePickers" : "setProxyValuePickers");
				section.setVariable("setValuePicker", property.getSetValuePickerMethod());
				section.setVariable("setValuePickerName", property.getSetValuePickerMethodName());
				section.setVariable("valueType", property.getValueType().getSimpleTypeName());
				section.setVariable("rendererType", property.getProxyRendererType());
				if (property.isProxy() || property.isCollectionOfProxy()) {
					String propTypeName = StringUtils.uncapitalize(property.isCollectionOfProxy() ? method.getReturnType().getParameters().get(0).getSimpleTypeName() : method.getReturnType().getSimpleTypeName());
					propTypeName = propTypeName.substring(0, propTypeName.indexOf("Proxy"));
					section.setVariable("requestInterface", propTypeName + "Request");
					section.setVariable("findMethod", "find" + StringUtils.capitalize(propTypeName) + "Entries(0, 50)");
				}
                          maybeAddImport(dataDictionary, importSet, property.getPropertyType());
                          if (property.isCollectionOfProxy()) {
                             maybeAddImport(dataDictionary, importSet,
                                 property.getPropertyType().getParameters().get(0));
                             maybeAddImport(dataDictionary, importSet, property.getSetEditorType());
                          }

                        }

		}

		dataDictionary.setVariable("proxyFields", proxyFields);

		// Add a section for the mobile properties.
		if (primaryProp != null) {
			dataDictionary.setVariable("primaryProp", primaryProp.getName());
			dataDictionary.setVariable("primaryPropGetter", primaryProp.getGetter());
			dataDictionary.setVariable("primaryPropBuilder", primaryProp.forMobileListView("primaryRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", primaryProp.getName());
			section.setVariable("propGetter", primaryProp.getGetter());
			section.setVariable("propType", primaryProp.getType());
			section.setVariable("propRenderer", primaryProp.getRenderer());
			section.setVariable("propRendererName", "primaryRenderer");
		} else {
			dataDictionary.setVariable("primaryProp", "id");
			dataDictionary.setVariable("primaryPropGetter", "getId");
			dataDictionary.setVariable("primaryPropBuilder", "");
		}
		if (secondaryProp != null) {
			dataDictionary.setVariable("secondaryPropBuilder", secondaryProp.forMobileListView("secondaryRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", secondaryProp.getName());
			section.setVariable("propGetter", secondaryProp.getGetter());
			section.setVariable("propType", secondaryProp.getType());
			section.setVariable("propRenderer", secondaryProp.getRenderer());
			section.setVariable("propRendererName", "secondaryRenderer");
		} else {
			dataDictionary.setVariable("secondaryPropBuilder", "");
		}
		if (dateProp != null) {
			dataDictionary.setVariable("datePropBuilder", dateProp.forMobileListView("dateRenderer"));
			TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
			section.setVariable("prop", dateProp.getName());
			section.setVariable("propGetter", dateProp.getGetter());
			section.setVariable("propType", dateProp.getType());
			section.setVariable("propRenderer", dateProp.getRenderer());
			section.setVariable("propRendererName", "dateRenderer");
		} else {
			dataDictionary.setVariable("datePropBuilder", "");
		}
		return dataDictionary;
	}

  private void maybeAddImport(TemplateDataDictionary dataDictionary,
      Set<String> importSet, JavaType type) {
    if (!importSet.contains(type.getFullyQualifiedTypeName())) {
            addImport(dataDictionary, type.getFullyQualifiedTypeName());
            importSet.add(type.getFullyQualifiedTypeName());
    }
  }

  private boolean isReadOnly(String name) {
    return name.equals(idPropertyName.getSymbolName()) || name.equals(versionPropertyName.getSymbolName());
  }

  private void buildListViewUiXml() {
		try {
			MirrorType destType = MirrorType.LIST_VIEW;
			String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
			writeWithTemplate(destFile, destType, "ListViewUiXml");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildMobileDetailsView() {
		try {
			MirrorType destType = MirrorType.MOBILE_DETAILS_VIEW;
			writeWithTemplate(destType);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildMobileDetailsViewUiXml() {
		try {
			MirrorType destType = MirrorType.MOBILE_DETAILS_VIEW;
			String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
			writeWithTemplate(destFile, destType, "MobileDetailsViewUiXml");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildDetailsView() {
		try {
			MirrorType destType = MirrorType.DETAILS_VIEW;
			writeWithTemplate(destType);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildDetailsViewUiXml() {
		try {
			MirrorType destType = MirrorType.DETAILS_VIEW;
			String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
			writeWithTemplate(destFile, destType, "DetailsViewUiXml");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildEditRenderer() {
		try {
			writeWithTemplate(MirrorType.EDIT_RENDERER);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildEditView() {
		try {
			MirrorType type = MirrorType.EDIT_VIEW;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);
			writeWithTemplate(type, dataDictionary, type.getTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildMobileEditView() {
		try {
			MirrorType type = MirrorType.MOBILE_EDIT_VIEW;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);
			writeWithTemplate(type, dataDictionary, type.getTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildEditViewUiXml() {
		try {
			MirrorType destType = MirrorType.EDIT_VIEW;
			String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
			writeWithTemplate(destFile, destType, "EditViewUiXml");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildMobileEditViewUiXml() {
		try {
			MirrorType destType = MirrorType.MOBILE_EDIT_VIEW;
			String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
			writeWithTemplate(destFile, destType, "MobileEditViewUiXml");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

        private void buildSetEditor() {
		try {
			MirrorType type = MirrorType.SET_EDITOR;
			writeWithTemplate(type, type.getTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

        private void buildSetEditorUiXml() {
		try {
			MirrorType destType = MirrorType.SET_EDITOR;
			String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
			writeWithTemplate(destFile, destType, "SetEditorUiXml");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void write(String destFile, String newContents, FileManager fileManager) {
		// Write to disk, or update a file if it is already present
		MutableFile mutableFile = null;
		if (fileManager.exists(destFile)) {
			// First verify if the file has even changed
			File f = new File(destFile);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {
			}

			if (!newContents.equals(existing)) {
				mutableFile = fileManager.updateFile(destFile);
			}
		} else {
			mutableFile = fileManager.createFile(destFile);
			Assert.notNull(mutableFile, "Could not create output file '" + destFile + "'");
		}

		try {
			if (mutableFile != null) {
				// If mutableFile was null, that means the source == destination content
				FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
	}

	private void buildRequest() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.REQUEST);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();
		// @Service(Employee.class)
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.Service")));
		
		List<ConstructorMetadataBuilder> constructors = new ArrayList<ConstructorMetadataBuilder>();
		List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<JavaType> extendsTypes = Collections.singletonList(new JavaType("com.google.gwt.requestfactory.shared.RequestContext"));
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		buildStaticRequestMethod(destinationMetadataId, methods, countMethod);
		buildStaticRequestMethod(destinationMetadataId, methods, findAllMethod);
		buildStaticRequestMethod(destinationMetadataId, methods, findEntriesMethod);
		buildStaticRequestMethod(destinationMetadataId, methods, findMethod);

		buildInstanceRequestMethod(destinationMetadataId, methods, entityMetadata.getRemoveMethod());
                buildInstanceRequestMethod(destinationMetadataId, methods, entityMetadata.getPersistMethod());

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(destinationMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);
		typeDetailsBuilder.setAnnotations(typeAnnotations);
		typeDetailsBuilder.setDeclaredConstructors(constructors);
		typeDetailsBuilder.setDeclaredFields(fields);
		typeDetailsBuilder.setDeclaredMethods(methods);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		typeDetailsBuilder.setImplementsTypes(implementsTypes);
		this.request = typeDetailsBuilder.build();
	}

  private void buildInstanceRequestMethod(String destinationMetadataId,
      List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData) {
    // com.google.gwt.requestfactory.shared.InstanceRequest remove()
    List<JavaType> methodReturnTypeArgs = Arrays.asList(new JavaType[] { getDestinationJavaType(MirrorType.PROXY), JavaType.VOID_OBJECT });
    JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.InstanceRequest", 0, DataType.TYPE, null, methodReturnTypeArgs);

    buildRequestMethod(destinationMetadataId, methods, methodMetaData, methodReturnType);
  }

	private void buildStaticRequestMethod(String destinationMetadataId, List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData) {
	  // com.google.gwt.requestfactory.shared.Request<List<EmployeeProxy>> findAllEmployees();
	  List<JavaType> methodReturnTypeArgs = Collections.singletonList(getGwtSideMethodType(methodMetaData.getReturnType()));
	  JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.Request", 0, DataType.TYPE, null, methodReturnTypeArgs);
	  
		buildRequestMethod(destinationMetadataId, methods, methodMetaData, methodReturnType);
	}

  private void buildRequestMethod(String destinationMetadataId,
      List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData,
      JavaType methodReturnType) {
    JavaSymbolName methodName = methodMetaData.getMethodName();

    List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>(methodMetaData.getParameterNames());

		List<AnnotatedJavaType> paramTypes = methodMetaData.getParameterTypes();

		for (AnnotatedJavaType paramType : paramTypes) {
			JavaType jtype = paramType.getJavaType();
			if (methodName.equals(findMethod.getMethodName())) {
				jtype = entityMetadata.getIdentifierField().getFieldType();
			}
			methodParameterTypes.add(jtype);
		}

		methods.add(new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder()));
  }

	/**
	 * Return the type arg for the client side method, given the domain method return type.
	 * if domainMethodReturnType is List<Integer> or Set<Integer>, returns the same.
	 * if domainMethodReturnType is List<Employee>, return List<EmployeeProxy>
	 */
	private JavaType getGwtSideMethodType(JavaType domainMethodReturnType) {
	  List<JavaType> typeParameters = domainMethodReturnType.getParameters();
	  if (typeParameters == null || typeParameters.size() == 0) {
	    PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(domainMethodReturnType, Path.SRC_MAIN_JAVA));
	    return getGwtSideLeafType(domainMethodReturnType, ptmd);
	  }
    List<JavaType> clientMethodTypeParameters = new ArrayList<JavaType>();
    for (JavaType domainSideType : typeParameters) {
      clientMethodTypeParameters.add(getGwtSideMethodType(domainSideType));
    }
	  return new JavaType(domainMethodReturnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, clientMethodTypeParameters);
  }

  class ExportedMethod {
		JavaSymbolName operationName; // Mandatory
		JavaSymbolName methodName; // Mandatory
		JavaType returns; // Mandatory
		List<AnnotatedJavaType> args; // Mandatory, but can be empty
		boolean isList;
	}

	/**
	 * @param mirrorType the mirror class we're producing (required)
	 * @return the MID to the mirror class applicable for the current governor (never null)
	 */
	private String getDestinationMetadataId(MirrorType mirrorType) {
		return PhysicalTypeIdentifier.createIdentifier(mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, governorTypeDetails.getName()), mirrorTypePath);
	}

	private JavaType getDestinationJavaType(JavaType physicalType, MirrorType mirrorType) {
		return mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, physicalType);
	}

	/**
	 * @param mirrorType the mirror class we're producing (required)
	 * @return the Java type the mirror class applicable for the current governor (never null)
	 */
	private JavaType getDestinationJavaType(MirrorType mirrorType) {
		return PhysicalTypeIdentifier.getJavaType(getDestinationMetadataId(mirrorType));
	}

	/**
	 * @param sharedType the shared type to lookup(required)
	 * @return the Java type the shared type applicable for the current project (never null)
	 */
	private JavaType getDestinationJavaType(SharedType sharedType) {
		String packageName = sharedType.getPath().packageName(projectMetadata);
		String typeName = sharedType.getFullName();
		return new JavaType(packageName + "." + typeName);
	}

	private AnnotationMetadataBuilder createAdditionalAnnotation(JavaType serverType) {
		List<AnnotationAttributeValue<?>> serverTypeAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		serverTypeAttributes.add(new ClassAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName()));
		return new AnnotationMetadataBuilder(serverType, serverTypeAttributes);
	}

	/**
	 * @return a newly-created type annotations list, complete with the @RooGwtMirroredFrom annotation properly setup
	 */
	private List<AnnotationMetadataBuilder> createAnnotations() {
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> rooGwtMirroredFromConfig = new ArrayList<AnnotationAttributeValue<?>>();
		rooGwtMirroredFromConfig.add(new ClassAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName()));
		annotations.add(new AnnotationMetadataBuilder(new JavaType(RooGwtMirroredFrom.class.getName()), rooGwtMirroredFromConfig));
		return annotations;
	}

	public ClassOrInterfaceTypeDetails getKey() {
		return proxy;
	}

	public ClassOrInterfaceTypeDetails getDetails() {
		return details;
	}

	public ClassOrInterfaceTypeDetails getListView() {
		return listView;
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
