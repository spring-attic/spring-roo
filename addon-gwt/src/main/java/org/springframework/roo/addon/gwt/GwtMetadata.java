package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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
	private boolean versionIntegerOnServerSide = true;
	private boolean idLongOnServerSide = false;

	private ClassOrInterfaceTypeDetails details;
	private ClassOrInterfaceTypeDetails listView;

	private DefaultClassOrInterfaceTypeDetails listViewBinder;
	private DefaultClassOrInterfaceTypeDetails detailsViewBinder;
	private DefaultClassOrInterfaceTypeDetails editViewBinder;

	public GwtMetadata(String identifier, MirrorTypeNamingStrategy mirrorTypeNamingStrategy,
            ProjectMetadata projectMetadata, ClassOrInterfaceTypeDetails governorTypeDetails,
            Path mirrorTypePath, BeanInfoMetadata beanInfoMetadata,
            EntityMetadata entityMetadata, FileManager fileManager,
            MetadataService metadataService) {
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
		// TODO (cromwellian) Argh! Why must I make this an outer class!
		listViewBinder = buildListViewBinder(MirrorType.LIST_VIEW_BINDER, MirrorType.LIST_VIEW);
		detailsViewBinder = buildListViewBinder(MirrorType.DETAILS_VIEW_BINDER, MirrorType.DETAILS_VIEW);
		editViewBinder = buildListViewBinder(MirrorType.EDIT_VIEW_BINDER, MirrorType.EDIT_VIEW);
		buildEditActivity();
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
		result.add(listViewBinder);
		result.add(detailsViewBinder);
		result.add(editViewBinder);
		result.add(request);
		return result;
	}

	private void resolveEntityInformation() {
		if (entityMetadata != null && entityMetadata.isValid()) {
			// Lookup special fields
			versionPropertyName = entityMetadata.getVersionField().getFieldName();
			if (entityMetadata.getVersionField().getFieldType().equals(JavaType.INT_OBJECT)) {
				versionIntegerOnServerSide = true;
			}
			idPropertyName = entityMetadata.getIdentifierField().getFieldName();
			if (entityMetadata.getIdentifierField().getFieldType().equals(JavaType.LONG_OBJECT)) {
				idLongOnServerSide = true;
			}

			// Lookup the "find all" method and store it
			findAllMethod = entityMetadata.getFindAllMethod();
			findMethod = entityMetadata.getFindMethod();
			findEntriesMethod = entityMetadata.getFindEntriesMethod();
			countMethod = entityMetadata.getCountMethod();
			Assert.notNull(findAllMethod, "Find all method unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
		}
	}

	private void buildActivitiesMapper() {
		try {
			MirrorType type = MirrorType.ACTIVITIES_MAPPER;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
			addReference(dataDictionary, MirrorType.DETAIL_ACTIVITY);
			addReference(dataDictionary, MirrorType.EDIT_ACTIVITY);
			writeWithTemplate(type, dataDictionary, type.getTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildProxy() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.PROXY);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		// @ProxyFor(Employee.class)
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ProxyFor")));
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// attribs.add(new ClassAttributeValue(new JavaSymbolName("type"), beanInfoMetadata.getJavaBean()));
		// attribs.add(new StringAttributeValue(new JavaSymbolName("token"), governorTypeDetails.getName().getSimpleTypeName()));
		// typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.ServerType"), attribs));

		// extends Proxy
		extendsTypes.add(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"));

		// Decide fields we'll be mapping
		SortedMap<JavaSymbolName, JavaType> propToGwtSideType = new TreeMap<JavaSymbolName, JavaType>();
		Map<JavaSymbolName, JavaType> propToWrapperType = new HashMap<JavaSymbolName, JavaType>();
		if (beanInfoMetadata != null) {
			for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors()) {
				JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor).getSymbolName()));

				JavaType gwtSideType = null;
				JavaType wrapperType = new JavaType("com.google.gwt.requestfactory.shared.Property");
                                JavaType enumWrapperType = new JavaType("com.google.gwt.requestfactory.shared.EnumProperty");
				// TODO id and version excluded as they specified in the Proxy interface. Revisit later
				if ("id".equals(propertyName.getSymbolName()) || "version".equals(propertyName.getSymbolName())) {
					wrapperType = null;
				}

				JavaType returnType = accessor.getReturnType();
                                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(returnType, Path.SRC_MAIN_JAVA));
                                boolean isEnum = ptmd != null && ptmd.getPhysicalTypeDetails() != null && ptmd.getPhysicalTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;

				boolean isDomainObject = !isEnum && !isShared(returnType) && !(returnType.equals(JavaType.BOOLEAN_OBJECT) || returnType.equals(JavaType.INT_OBJECT) || returnType.isPrimitive() ||
                                    returnType.equals(JavaType.LONG_OBJECT) || returnType.equals(JavaType.STRING_OBJECT) || returnType.equals(JavaType.DOUBLE_OBJECT) || returnType.equals(JavaType.FLOAT_OBJECT) || returnType.equals(new JavaType("java.util.Date")));
				if (isDomainObject) {
					gwtSideType = getDestinationJavaType(returnType, MirrorType.PROXY);
				} else {
					gwtSideType = returnType;
                                        if (returnType.isPrimitive()) {
                                          if (returnType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
                                            gwtSideType = JavaType.BOOLEAN_OBJECT;
                                          }
                                          if (returnType.equals(JavaType.INT_PRIMITIVE)) {
                                            gwtSideType = JavaType.INT_OBJECT;
                                          }if (returnType.equals(JavaType.DOUBLE_PRIMITIVE)) {
                                            gwtSideType = JavaType.DOUBLE_OBJECT;
                                          }if (returnType.equals(JavaType.LONG_PRIMITIVE)) {
                                            gwtSideType = JavaType.LONG_OBJECT;
                                          }
                                        }
					// Handle the identifier special case
					if (idPropertyName.equals(propertyName) && idLongOnServerSide) {
						gwtSideType = JavaType.LONG_OBJECT;
					}
					// Handle the version special case
					if (versionPropertyName.equals(propertyName) && versionIntegerOnServerSide) {
						gwtSideType = JavaType.INT_OBJECT;
					}
					// TODO: (cromwellian) HACK! handle foreign-id refs, we assume java.lang.Long is an id
					if (gwtSideType.getFullyQualifiedTypeName().equals("java.lang.Long") && idLongOnServerSide) {
						gwtSideType = JavaType.LONG_OBJECT;
					}

				}

                                if (isEnum) {
                                  wrapperType = enumWrapperType;
                                }
				if (wrapperType == null) {
					// This field won't be supported
					continue;
				}

				// Store in the maps
				propToGwtSideType.put(propertyName, gwtSideType);
				propToWrapperType.put(propertyName, wrapperType);
			}
		}

		FieldMetadata tokenField = new DefaultFieldMetadata(destinationMetadataId, Modifier.PUBLIC, new JavaSymbolName("TOKEN"), JavaType.STRING_OBJECT, "\"" + name.getSimpleTypeName() + "\"", new ArrayList<AnnotationMetadata>());
		fields.add(tokenField);

		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaSymbolName fieldName = propertyName;
			List<JavaType> fieldArgs = new ArrayList<JavaType>();
			fieldArgs.add(propToGwtSideType.get(propertyName));

			JavaType fieldType = new JavaType(new JavaType("com.google.gwt.requestfactory.shared.Property").getFullyQualifiedTypeName(), 0, DataType.TYPE, null, fieldArgs);
                        JavaType rhsType = new JavaType(propToWrapperType.get(propertyName).getFullyQualifiedTypeName(), 0, DataType.TYPE, null, fieldArgs);

                  String clazz = propToGwtSideType.get(propertyName)
                      .getFullyQualifiedTypeName();
                  boolean isEnumProp = rhsType.getSimpleTypeName()
                      .equals("EnumProperty");
                  String fieldInitializer = "new " + rhsType + "(\""
                      + propertyName.getSymbolName() + ("\", "
                      + (isEnumProp ? "" : "\"" + propertyName.getReadableSymbolName() + "\", ") + clazz + ".class"
                            +(isEnumProp ? ", " + clazz + ".values())" : ")"));
			FieldMetadata fieldMetadata = new DefaultFieldMetadata(destinationMetadataId, Modifier.INTERFACE, fieldName, fieldType, fieldInitializer, null);
			fields.add(fieldMetadata);
		}

		// Getter methods for EmployeeProxy
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaType methodReturnType = propToGwtSideType.get(propertyName);
			JavaSymbolName methodName = new JavaSymbolName("get" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
			List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();

			// Potentially add GWT's annotation helpers
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			MethodMetadata methodMetadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, annotations, null, null);
			methods.add(methodMetadata);
		}

		// isChanged method
		methods.add(new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, new JavaSymbolName("isChanged"), JavaType.BOOLEAN_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(new ArrayList<JavaType>()), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), null, null));

		// Setter methods for EmployeeProxy
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaType methodReturnType = JavaType.VOID_PRIMITIVE;
			// propToGwtSideType.get(propertyName);
			JavaSymbolName methodName = new JavaSymbolName("set" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = Collections.<JavaType> singletonList(propToGwtSideType.get(propertyName));
			List<JavaSymbolName> methodParameterNames = Collections.<JavaSymbolName> singletonList(propertyName);

			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			MethodMetadata methodMetadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, annotations, null, null);
			methods.add(methodMetadata);
		}

		this.proxy = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
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

	private void buildEditActivity() {
		try {
			MirrorType type = MirrorType.EDIT_ACTIVITY;
			TemplateDataDictionary dataDictionary = buildDataDictionary(type);
			addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
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
			addReference(dataDictionary, MirrorType.DETAILS_VIEW);
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
			addReference(dataDictionary, MirrorType.LIST_VIEW);
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
		dataDictionary.setVariable("name", governorTypeDetails.getName().getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
		dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());

		for (FieldMetadata field : proxy.getDeclaredFields()) {
			if (field.getFieldName().getSymbolName().equals("TOKEN")) {
				continue;
			}
			dataDictionary.addSection("fields").setVariable("field", field.getFieldName().getSymbolName());
		}
                String displayFields = null, displayFieldGetter = null;
                Set<String> importSet = new HashSet<String>();
		for (MethodMetadata method : proxy.getDeclaredMethods()) {
			if (!method.getMethodName().getSymbolName().startsWith("get")) {
				continue;
			}
			String getter = method.getMethodName().getSymbolName();
                  PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(
                      PhysicalTypeIdentifier.createIdentifier(method.getReturnType(),
                          Path.SRC_MAIN_JAVA));
			Property property = new Property(getter, StringUtils.uncapitalize(getter.substring(3)), "set" + getter.substring(3), method.getReturnType(), ptmd);

                        if (property.isString()) {
                          displayFieldGetter = property.getGetter();
                        }
                        if (property.isProxy()) {
                          if (displayFields != null) {
                            displayFields += ", ";
                          } else {
                            displayFields = "";
                          }
                          displayFields += "\"" + property.getName() + "\"";
                        }

			dataDictionary.addSection("props1").setVariable("prop", property.forDetailsView());
			dataDictionary.addSection("props2").setVariable("prop", property.forEditView());
			dataDictionary.addSection("props3").setVariable("prop", property.forDetailsUIXml());
			dataDictionary.addSection("props4").setVariable("prop", property.forListView(proxyType.getSimpleTypeName()));
			dataDictionary.addSection("props5").setVariable("prop", property.forEditUiXml());

            dataDictionary.setVariable("proxyRendererType", MirrorType.EDIT_RENDERER.getPath().packageName(projectMetadata)
              + "." + proxy.getName().getSimpleTypeName() + "Renderer");

                        if (property.isProxy() || property.isEnum()) {
                          TemplateDataDictionary section = dataDictionary.addSection(property.isEnum() ? "setEnumValuePickers" : "setProxyValuePickers");
                          section.setVariable("setValuePicker", property.getSetValuePickerMethod());
                          section.setVariable("setValuePickerName", property.getSetValuePickerMethodName());
                          section.setVariable("valueType", property.getPropertyType().getSimpleTypeName());
                          section.setVariable("rendererType", property.getRendererType());
                          if (property.isProxy()) {
                            String propTypeName = StringUtils.uncapitalize(method.getReturnType().getSimpleTypeName());
                            propTypeName = propTypeName.substring(0, propTypeName.indexOf("Proxy"));
                            section.setVariable("requestInterface", propTypeName+"Request");
                            section.setVariable("findMethod", "find"+StringUtils.capitalize(propTypeName)+"Entries(0, 50)");
                          }
                          if (!importSet.contains(property.getType())) {
                            addImport(dataDictionary, property.getType());
                            importSet.add(property.getType());
                          }
                        }

		}
                if (displayFieldGetter == null) {
                  displayFieldGetter = "getId";
                }
                dataDictionary.setVariable("displayFields", displayFields);
                dataDictionary.setVariable("displayFieldGetter", displayFieldGetter);
		return dataDictionary;
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

	private DefaultClassOrInterfaceTypeDetails buildListViewBinder(MirrorType binderMirrorType, MirrorType viewType) {
		String destinationMetadataId = getDestinationMetadataId(binderMirrorType);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// private static final Binder BINDER = GWT.create(Binder.class)
		List<JavaType> binderParams = new ArrayList<JavaType>();
		binderParams.add(new JavaType("com.google.gwt.user.client.ui.HTMLPanel"));
		binderParams.add(getDestinationJavaType(viewType));
		JavaType binderType = new JavaType("com.google.gwt.uibinder.client.UiBinder", 0, DataType.TYPE, null, binderParams);
		extendsTypes.add(binderType);
		return new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildRequest() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.REQUEST);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		// @Service(Employee.class)
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.Service")));
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();
		buildRequestMethod(destinationMetadataId, methods, findAllMethod);
		buildRequestMethod(destinationMetadataId, methods, findMethod);
		buildRequestMethod(destinationMetadataId, methods, countMethod);
		buildRequestMethod(destinationMetadataId, methods, findEntriesMethod);

		// remove(EmployeeProxy proxy) and persist(EmployeeProxy proxy) methods.
		JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.RequestObject", 0, DataType.TYPE, null, Collections.singletonList(JavaType.VOID_OBJECT));
		for (MethodMetadata metadata : new MethodMetadata[] { entityMetadata.getRemoveMethod(), entityMetadata.getPersistMethod() }) {
			List<AnnotatedJavaType> parameterTypes = Collections.singletonList(new AnnotatedJavaType(getDestinationJavaType(MirrorType.PROXY), null));
			List<JavaSymbolName> parameterNames = Collections.singletonList(new JavaSymbolName("proxy"));
			List<AnnotationMetadata> annotations = Collections.singletonList((AnnotationMetadata) new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.Instance"), Collections.<AnnotationAttributeValue<?>> emptyList()));
			MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, metadata.getMethodName(), methodReturnType, parameterTypes, parameterNames, annotations, null, null);
			methods.add(method1Metadata);
		}

		this.request = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildRequestMethod(String destinationMetadataId, List<MethodMetadata> methods, MethodMetadata methodMetaData) {
		// com.google.gwt.requestfactory.shared.EntityListRequest<EmployeeKey> findAllEmployees();
		JavaSymbolName method1Name = methodMetaData.getMethodName();
		List<JavaType> method1ReturnTypeArgs0 = new ArrayList<JavaType>();
		boolean isList = methodMetaData.getReturnType().getFullyQualifiedTypeName().equals("java.util.List");
		method1ReturnTypeArgs0.add(method1Name.getSymbolName().startsWith("count") ? JavaType.LONG_OBJECT : getDestinationJavaType(MirrorType.PROXY));
		JavaType method1ReturnType = new JavaType(method1Name.getSymbolName().startsWith("count") ? "com.google.gwt.requestfactory.shared.RequestObject" : (isList ? "com.google.gwt.requestfactory.shared.ProxyListRequest" : "com.google.gwt.requestfactory.shared.ProxyRequest"), 0, DataType.TYPE, null, method1ReturnTypeArgs0);
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		method1ParameterNames.addAll(methodMetaData.getParameterNames());
		List<AnnotatedJavaType> paramTypes = methodMetaData.getParameterTypes();

		for (int i = 0; i < paramTypes.size(); i++) {
			List<JavaType> typeParams = new ArrayList<JavaType>();
			JavaType jtype = paramTypes.get(i).getJavaType();
			if (method1Name.equals(findMethod.getMethodName())) {
				jtype = JavaType.LONG_OBJECT;
			}
			typeParams.add(jtype);
			JavaType propRef = new JavaType("com.google.gwt.requestfactory.shared.PropertyReference", 0, DataType.TYPE, null, typeParams);
			method1ParameterTypes.add(jtype.isPrimitive() ? jtype : propRef);
		}

		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, null, null, null);
		methods.add(method1Metadata);
	}

        // TODO (cromwellian): class is getting ugly, clean this up
	public class Property {
		private String name;
		private String getter;
		private String setter;
		private JavaType type;

          private PhysicalTypeMetadata ptmd;

          public Property(String getter, String name, String setter) {
			this.getter = getter;
			this.name = name;
			this.setter = setter;
		}

		public Property(String getter, String name, String setting, JavaType returnType,
                    PhysicalTypeMetadata ptmd) {
			this(getter, name, setting);
			this.type = returnType;
                  this.ptmd = ptmd;
                }

		public String getName() {
			return name;
		}

		public String getSetter() {
			return setter;
		}

		public String getGetter() {
			return getter;
		}

		public String getType() {
			return type.getFullyQualifiedTypeName();
		}

                public JavaType getPropertyType() {
			return type;
		}

		public void setGetter(String getter) {
			this.getter = getter;
		}

                public boolean isBoolean() {
                        return type != null && type.equals(JavaType.BOOLEAN_OBJECT);
                }

		public boolean isDate() {
			return type != null && type.equals(new JavaType("java.util.Date"));
		}

                public boolean isPrimitive() {
                        return type.isPrimitive() || isDate() || isString()|| type.equals(JavaType.DOUBLE_OBJECT) || type.equals(JavaType.LONG_OBJECT) || type.equals(JavaType.INT_OBJECT) || isBoolean();
                }

                public boolean isString() {
                        return type != null && type.equals(new JavaType("java.lang.String"));
                }

		public String getBinder() {
			if (type.equals(JavaType.DOUBLE_OBJECT)) {
				return "app:DoubleBox";
			}
			if (type.equals(JavaType.LONG_OBJECT)) {
				return "app:LongBox";
			}
			if (type.equals(JavaType.INT_OBJECT)) {
				return "app:IntegerBox";
			}
			return isDate() ? "d:DateBox" : isBoolean() ? "g:CheckBox" : isString() ? "g:TextBox" : "g:ValueListBox";
		}

		public String getEditor() {
			if (type.equals(JavaType.DOUBLE_OBJECT)) {
				return "DoubleBox";
			}
			if (type.equals(JavaType.LONG_OBJECT)) {
				return "LongBox";
			}
			if (type.equals(JavaType.INT_OBJECT)) {
				return "IntegerBox";
			}
                        if (isBoolean()) {
                                return "(provided = true) CheckBox";
                        }
			return isDate() ? "DateBox" : isString() ? "TextBox" : "(provided = true) ValueListBox<" + type.getFullyQualifiedTypeName() + ">";
		}

		public String getFormatter() {
			return isDate() ? "DateTimeFormat.getShortDateFormat().format(" : isProxy() ? getRendererType() + ".instance().render(" : "String.valueOf(";
		}

		public String getRenderer() {
			return isDate() ? "new DateTimeFormatRenderer(DateTimeFormat.getShortDateFormat())" : isPrimitive() || isEnum() ?
                            "new AbstractRenderer<" + getType() + ">() {\n      public String render(" + getType() + " obj) {\n        return obj == null ? \"\" : String.valueOf(obj);\n      }    \n}" :
                            getRendererType() + ".instance()";
		}

          private String getRendererType() {
            return
                MirrorType.EDIT_RENDERER.getPath().packageName(projectMetadata)
                    + "." + type.getSimpleTypeName() + "Renderer";
          }

          public String getCheckboxSubtype() {
            // TODO: Ugly hack, fix in M4
              return "new CheckBox() { public void setValue(Boolean value) { super.setValue(value == null ? Boolean.FALSE : value); } }";  
          }

          public String getReadableName() {
			return new JavaSymbolName(name).getReadableSymbolName();
		}

		public String forDetailsView() {
			return new StringBuilder(getName()).append(".setInnerText(").append(getFormatter()).append("proxy.").append(getGetter()).append("()));").toString();
		}

    public String forEditView() {
      String initializer = "";

      if (isBoolean()) {
        initializer = " = " + getCheckboxSubtype();
      }

      if (isEnum()) {
        initializer = String.format(" = new ValueListBox<%s>(%s)", type.getFullyQualifiedTypeName(), getRenderer());
      }

      if (isProxy()) {
        initializer = String.format(" = new ValueListBox<%1$s>(%2$s.instance(), " +
        		"new com.google.gwt.app.place.EntityProxyKeyProvider<%1$s>())", type.getFullyQualifiedTypeName(), getRendererType());
      }

      return String.format("@UiField %s %s %s", getEditor(), getName(), initializer);
    }

		public String forDetailsUIXml() {
			return new StringBuilder("<tr><td><div class='{style.label}'>").append(getReadableName()).append(":</div></td><td><span ui:field='").append(getName()).append("'></span></td></tr>").toString();
		}

		public String forListView(String proxy) {
			return new StringBuilder("columns.add(new PropertyColumn<").append(proxy).append(", ").append(getType()).append(">(").append(proxy).append(".").append(getName()).append(", ").append(getRenderer()).append("));").toString();
		}

		public String forEditUiXml() {
			return new StringBuilder("<tr><td><div class='{style.label}'>").append(getReadableName()).append(":</div></td><td><").append(getBinder()).append(" ui:field='").append(getName()).append("'></").append(getBinder()).append("></td></tr>").toString();
		}

          public boolean isProxy() {
            return !isDate() && !isString() && !isPrimitive() && !isEnum();
          }

          private boolean isEnum() {
            return ptmd != null && ptmd.getPhysicalTypeDetails() != null && ptmd.getPhysicalTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
          }

          public String getSetValuePickerMethod() {
            return "\tpublic void " + getSetValuePickerMethodName()
                + "(Collection<" +type.getSimpleTypeName()+"> values) {\n"+
                "\t\t"+getName() + ".setAcceptableValues(values);\n" +
                "\t}\n";      
          }

          private String getSetValuePickerMethodName() {
            return "set" + StringUtils.capitalize(getName()) + "PickerValues";
          }
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

	private AnnotationMetadata createAdditionalAnnotation(JavaType serverType) {
		List<AnnotationAttributeValue<?>> serverTypeAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		serverTypeAttributes.add(new ClassAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName()));
		return new DefaultAnnotationMetadata(serverType, serverTypeAttributes);
	}

	/**
	 * @return a newly-created type annotations list, complete with the @RooGwtMirroredFrom annotation properly setup
	 */
	private List<AnnotationMetadata> createAnnotations() {
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> rooGwtMirroredFromConfig = new ArrayList<AnnotationAttributeValue<?>>();
		rooGwtMirroredFromConfig.add(new ClassAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName()));
		annotations.add(new DefaultAnnotationMetadata(new JavaType(RooGwtMirroredFrom.class.getName()), rooGwtMirroredFromConfig));
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
