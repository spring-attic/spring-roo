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
import java.util.HashSet;
import java.util.List;
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
		buildListView();
		buildListViewUiXml();
		buildDetailsView();
		buildDetailsViewUiXml();
		buildEditView();
		buildEditViewUiXml();
		buildEditRenderer();
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
			versionPropertyName = entityMetadata.getVersionField().getFieldName();
			idPropertyName = entityMetadata.getIdentifierField().getFieldName();

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
			addReference(dataDictionary, MirrorType.DETAIL_ACTIVITY);
			addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);
      addReference(dataDictionary, MirrorType.LIST_VIEW);
      addReference(dataDictionary, MirrorType.DETAILS_VIEW);
      addReference(dataDictionary, MirrorType.EDIT_VIEW);
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

		// Decide fields we'll be mapping
		SortedMap<JavaSymbolName, JavaType> propToGwtSideType = new TreeMap<JavaSymbolName, JavaType>();
		if (beanInfoMetadata != null) {
			for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors()) {
				JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor).getSymbolName()));
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
                    PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(returnType, Path.SRC_MAIN_JAVA));

                    JavaType gwtSideType = getGwtSideLeafType(returnType, ptmd);

                    // Store in the maps
                    propToGwtSideType.put(propertyName, gwtSideType);
                }
			}
		}

		// Getter methods for EmployeeProxy
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaType methodReturnType = propToGwtSideType.get(propertyName);
			JavaSymbolName methodName = new JavaSymbolName("get" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
			List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();
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
         || returnType.equals(new JavaType("java.util.Date"));
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
      addReference(dataDictionary, MirrorType.REQUEST);
      addReference(dataDictionary, MirrorType.EDIT_VIEW);
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
			writeWithTemplate(type, dataDictionary);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildListActivity() {
		try {
			MirrorType type = MirrorType.LIST_ACTIVITY;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
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
		dataDictionary.setVariable("uiPackage", GwtPath.GWT_SCAFFOLD_UI.packageName(projectMetadata));
		dataDictionary.setVariable("name", governorTypeDetails.getName().getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
    dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());

		String proxyFields = null;
		String displayField = null;
		String displayFieldGetter = null;
		Set<String> importSet = new HashSet<String>();
		for (MethodMetadata method : proxy.getDeclaredMethods()) {
			if (!GwtProxyProperty.isAccessor(method)) {
				continue;
			}
                        if ((destType.isUI()  && isCollectionType(method.getReturnType()))) {
                          continue;
                        }
                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(method.getReturnType(), Path.SRC_MAIN_JAVA));
			GwtProxyProperty property = new GwtProxyProperty(projectMetadata, method, ptmd);

			if (property.isString()) {
			  displayField = property.getName();
				displayFieldGetter = property.getGetter();
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
			dataDictionary.addSection("detailViewProps").setVariable("prop", property.forDetailsView());
			if (!isReadOnly(property.getName())) dataDictionary.addSection("editViewProps").setVariable("prop", property.forEditView());
			dataDictionary.addSection("detailsUiXmlProps").setVariable("prop", property.forDetailsUIXml());

			TemplateDataDictionary propertiesSection = dataDictionary.addSection("properties");
      propertiesSection.setVariable("prop", property.getName());
      propertiesSection.setVariable("propGetter", property.getGetter());
      propertiesSection.setVariable("propType", property.getType());
      propertiesSection.setVariable("propRenderer", property.getRenderer());
      propertiesSection.setVariable("propReadable", property.getReadableName());

      if (!isReadOnly(property.getName())) dataDictionary.addSection("editUiXmlProps").setVariable("prop", property.forEditUiXml());

			dataDictionary.setVariable("proxyRendererType", MirrorType.EDIT_RENDERER.getPath().packageName(projectMetadata) + "." + proxy.getName().getSimpleTypeName() + "Renderer");

			if (property.isProxy() || property.isEnum()) {
				TemplateDataDictionary section = dataDictionary.addSection(property.isEnum() ? "setEnumValuePickers" : "setProxyValuePickers");
				section.setVariable("setValuePicker", property.getSetValuePickerMethod());
				section.setVariable("setValuePickerName", property.getSetValuePickerMethodName());
				section.setVariable("valueType", property.getPropertyType().getSimpleTypeName());
				section.setVariable("rendererType", property.getProxyRendererType());
				if (property.isProxy()) {
					String propTypeName = StringUtils.uncapitalize(method.getReturnType().getSimpleTypeName());
					propTypeName = propTypeName.substring(0, propTypeName.indexOf("Proxy"));
					section.setVariable("requestInterface", propTypeName + "Request");
					section.setVariable("findMethod", "find" + StringUtils.capitalize(propTypeName) + "Entries(0, 50)");
				}
				if (!importSet.contains(property.getType())) {
					addImport(dataDictionary, property.getType());
					importSet.add(property.getType());
				}
			}

		}
		if (displayFieldGetter == null) {
		  displayField = "id";
			displayFieldGetter = "getId";
		}
		dataDictionary.setVariable("proxyFields", proxyFields);
    dataDictionary.setVariable("displayField", displayField);
    dataDictionary.setVariable("displayFieldGetter", displayFieldGetter);
		return dataDictionary;
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
			writeWithTemplate(type, type.getTemplate());
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
