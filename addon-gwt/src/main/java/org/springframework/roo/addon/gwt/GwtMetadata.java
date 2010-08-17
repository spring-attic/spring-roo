package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
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
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
public class GwtMetadata extends AbstractMetadataItem {
	private FileManager fileManager;
	private static final String PROVIDES_TYPE_STRING = GwtMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

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
	private ClassOrInterfaceTypeDetails recordChanged;
	private ClassOrInterfaceTypeDetails changeHandler;
	private ClassOrInterfaceTypeDetails request;

	private ClassOrInterfaceTypeDetails record;
	private JavaSymbolName idPropertyName;
	private JavaSymbolName versionPropertyName;
	private boolean versionIntegerOnServerSide = true;
	private boolean idLongOnServerSide = false;

	private ClassOrInterfaceTypeDetails details;
	private ClassOrInterfaceTypeDetails listView;

	private DefaultClassOrInterfaceTypeDetails listViewBinder;
	private DefaultClassOrInterfaceTypeDetails detailsViewBinder;
	private DefaultClassOrInterfaceTypeDetails editViewBinder;

	public GwtMetadata(String identifier, MirrorTypeNamingStrategy mirrorTypeNamingStrategy, ProjectMetadata projectMetadata, ClassOrInterfaceTypeDetails governorTypeDetails, Path mirrorTypePath, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FileManager fileManager) {
		super(identifier);
		this.mirrorTypeNamingStrategy = mirrorTypeNamingStrategy;
		this.projectMetadata = projectMetadata;
		this.governorTypeDetails = governorTypeDetails;
		this.mirrorTypePath = mirrorTypePath;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.fileManager = fileManager;

		// We know GwtMetadataProvider already took care of all the necessary checks. So we can just re-create fresh representations of the types we're responsible for
		resolveEntityInformation();
		buildRecordChanged();
		buildChangeHandler();
		buildRecord();
		buildActivitiesMapper();
		// TODO (cromwellian) Argh! Why must I make this an outer class!
		this.listViewBinder = buildListViewBinder(MirrorType.LIST_VIEW_BINDER, MirrorType.LIST_VIEW);
		this.detailsViewBinder = buildListViewBinder(MirrorType.DETAILS_VIEW_BINDER, MirrorType.DETAILS_VIEW);
		this.editViewBinder = buildListViewBinder(MirrorType.EDIT_VIEW_BINDER, MirrorType.EDIT_VIEW);
		buildEditActivity();
		buildDetailsActivity();
		buildListActivity();

		buildListView();
		buildListViewUiXml();
	//  buildDetailsView();
		buildDetailsViewHapax();
		buildDetailsViewUiXml();
		buildEditView();
		buildEditViewUiXml();
		buildRequest();
	}

	public List<ClassOrInterfaceTypeDetails> getAllTypes() {
		List<ClassOrInterfaceTypeDetails> result = new ArrayList<ClassOrInterfaceTypeDetails>();
		result.add(recordChanged);
		result.add(changeHandler);
		result.add(record);
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

	private void buildRecordChanged() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.RECORD_CHANGED);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// Shared Java Type: GwtEvent.Type<EmployeeChangedHandler>
		List<JavaType> gwtEventTypeParams = new ArrayList<JavaType>();
		gwtEventTypeParams.add(getDestinationJavaType(MirrorType.CHANGED_HANDLER));
		JavaType gwtEventType = new JavaType("com.google.gwt.event.shared.GwtEvent.Type", 0, DataType.TYPE, null, gwtEventTypeParams);

		// extends RecordChangedEvent<EmployeeRecord, EmployeeChangedHandler>
		List<JavaType> extParams = new ArrayList<JavaType>();
		extParams.add(getDestinationJavaType(MirrorType.RECORD));
		extParams.add(getDestinationJavaType(MirrorType.CHANGED_HANDLER));
		extendsTypes.add(new JavaType("com.google.gwt.valuestore.shared.RecordChangedEvent", 0, DataType.TYPE, null, extParams));

		// public static final Type<EmployeeChangedHandler> TYPE = new com.google.gwt.event.shared.GwtEvent.Type<EmployeeChangedHandler>();
		JavaType fieldType = gwtEventType;
		String fieldInitializer = "new " + fieldType.getNameIncludingTypeParameters() + "()";
		FieldMetadata fieldMetadata = new DefaultFieldMetadata(destinationMetadataId, Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL, new JavaSymbolName("TYPE"), fieldType, fieldInitializer, null);
		fields.add(fieldMetadata);

		// public EmployeeRecordChanged(com.springsource.extrack.gwt.request.EmployeeRecord record, com.google.gwt.valuestore.shared.WriteOperation) {
		// super(record, writeOperation);
		// }
		List<JavaType> constructorParameterTypes = new ArrayList<JavaType>();
		constructorParameterTypes.add(getDestinationJavaType(MirrorType.RECORD));
		constructorParameterTypes.add(new JavaType("com.google.gwt.valuestore.shared.WriteOperation"));
		List<JavaSymbolName> constructorParameterNames = new ArrayList<JavaSymbolName>();
		constructorParameterNames.add(new JavaSymbolName("record"));
		constructorParameterNames.add(new JavaSymbolName("writeOperation"));
		InvocableMemberBodyBuilder constructorBodyBuilder = new InvocableMemberBodyBuilder();
		constructorBodyBuilder.appendFormalLine("super(record, writeOperation);");
		ConstructorMetadata constructorMetadata = new DefaultConstructorMetadata(destinationMetadataId, Modifier.PUBLIC, AnnotatedJavaType.convertFromJavaTypes(constructorParameterTypes), constructorParameterNames, null, constructorBodyBuilder.getOutput());
		constructors.add(constructorMetadata);

		// public GwtEvent.Type<EmployeeChangedHandler> getAssociatedType() {
		// return TYPE;
		// }
		JavaSymbolName method1Name = new JavaSymbolName("getAssociatedType");
		JavaType method1ReturnType = gwtEventType;
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		InvocableMemberBodyBuilder method1BodyBuilder = new InvocableMemberBodyBuilder();
		method1BodyBuilder.appendFormalLine("return TYPE;");
		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, null, null, method1BodyBuilder.getOutput());
		methods.add(method1Metadata);

		// protected void dispatch(EmployeeChangedHandler handler) {
		// handler.onEmployeeChanged(this);
		// }
		JavaSymbolName method2Name = new JavaSymbolName("dispatch");
		JavaType method2ReturnType = JavaType.VOID_PRIMITIVE;
		List<JavaType> method2ParameterTypes = new ArrayList<JavaType>();
		method2ParameterTypes.add(getDestinationJavaType(MirrorType.CHANGED_HANDLER));
		List<JavaSymbolName> method2ParameterNames = new ArrayList<JavaSymbolName>();
		method2ParameterNames.add(new JavaSymbolName("handler"));
		InvocableMemberBodyBuilder method2BodyBuilder = new InvocableMemberBodyBuilder();
		method2BodyBuilder.appendFormalLine("handler." + getOnChangeMethod() + "(this);");
		MethodMetadata method2Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PROTECTED, method2Name, method2ReturnType, AnnotatedJavaType.convertFromJavaTypes(method2ParameterTypes), method2ParameterNames, null, null, method2BodyBuilder.getOutput());
		methods.add(method2Metadata);

		this.recordChanged = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildActivitiesMapper() {
		try {
			MirrorType type = MirrorType.ACTIVITIES_MAPPER;
			VelocityContext ctx = buildContext(type);
			addReference(ctx, MirrorType.DETAIL_ACTIVITY);
			addReference(ctx, MirrorType.EDIT_ACTIVITY);
			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildChangeHandler() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.CHANGED_HANDLER);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// extends com.google.gwt.event.shared.EventHandler
		extendsTypes.add(new JavaType("com.google.gwt.event.shared.EventHandler"));

		// void onEmployeeChanged(EmployeeRecordChanged event);
		JavaSymbolName method1Name = new JavaSymbolName(getOnChangeMethod());
		JavaType method1ReturnType = JavaType.VOID_PRIMITIVE;
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		method1ParameterTypes.add(getDestinationJavaType(MirrorType.RECORD_CHANGED));
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		method1ParameterNames.add(new JavaSymbolName("event"));
		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, null, null, null);
		methods.add(method1Metadata);

		this.changeHandler = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildRecord() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.RECORD);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		// @DataTransferObject(Employee.class)
    typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.DataTransferObject")));
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// attribs.add(new ClassAttributeValue(new JavaSymbolName("type"), beanInfoMetadata.getJavaBean()));
		// attribs.add(new StringAttributeValue(new JavaSymbolName("token"), governorTypeDetails.getName().getSimpleTypeName()));

		// typeAnnotations.add(new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.ServerType"), attribs));

		// extends Record
		extendsTypes.add(new JavaType("com.google.gwt.valuestore.shared.Record"));

		// Decide fields we'll be mapping
		SortedMap<JavaSymbolName, JavaType> propToGwtSideType = new TreeMap<JavaSymbolName, JavaType>();
		Map<JavaSymbolName, JavaType> propToWrapperType = new HashMap<JavaSymbolName, JavaType>();
		if (beanInfoMetadata != null) {
			for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors()) {
				JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor).getSymbolName()));

				JavaType gwtSideType = null;
				JavaType wrapperType = new JavaType("com.google.gwt.valuestore.shared.Property");

				// TODO id and version excluded as they specified in the Record interface. Revisit later
				if ("id".equals(propertyName.getSymbolName()) || "version".equals(propertyName.getSymbolName())) {
					wrapperType = null;
				}

				JavaType returnType = accessor.getReturnType();
				boolean isDomainObject = !(returnType.equals(JavaType.BOOLEAN_OBJECT) || returnType.equals(JavaType.INT_OBJECT) || returnType.equals(JavaType.LONG_OBJECT) || returnType.equals(JavaType.STRING_OBJECT) || returnType.equals(JavaType.DOUBLE_OBJECT) || returnType.equals(JavaType.FLOAT_OBJECT) || returnType.equals(new JavaType("java.util.Date")));
				if (isDomainObject) {
					gwtSideType = getDestinationJavaType(returnType, MirrorType.RECORD);
				} else {
					gwtSideType = returnType;
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

			JavaType fieldType = new JavaType(propToWrapperType.get(propertyName).getFullyQualifiedTypeName(), 0, DataType.TYPE, null, fieldArgs);
			String fieldInitializer = "new " + fieldType + "(\"" + propertyName.getSymbolName() + "\", \"" + propertyName.getReadableSymbolName() + "\", " + propToGwtSideType.get(propertyName).getFullyQualifiedTypeName() + ".class)";
			FieldMetadata fieldMetadata = new DefaultFieldMetadata(destinationMetadataId, Modifier.INTERFACE, fieldName, fieldType, fieldInitializer, null);
			fields.add(fieldMetadata);
		}

		// getter methods for EmployeeRecord
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaType methodReturnType = propToGwtSideType.get(propertyName);
			JavaSymbolName methodName = new JavaSymbolName("get" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
			List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();

			// Potentially add GWT's annotation helpers
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			// if (propertyName.equals(idPropertyName)) {
			// annotations.add(new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.Id"), new ArrayList<AnnotationAttributeValue<?>>()));
			// if (idLongOnServerSide) {
			// annotations.add(new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.LongString"), new ArrayList<AnnotationAttributeValue<?>>()));
			// }
			// }
			// if (propertyName.equals(versionPropertyName)) {
			// annotations.add(new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.Version"), new ArrayList<AnnotationAttributeValue<?>>()));
			// }

			MethodMetadata methodMetadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, annotations, null, null);
			methods.add(methodMetadata);
		}

		// isChanged method
    methods.add(new DefaultMethodMetadata(destinationMetadataId,
        Modifier.ABSTRACT, new JavaSymbolName("isChanged"),
        JavaType.BOOLEAN_PRIMITIVE,
        AnnotatedJavaType.convertFromJavaTypes(new ArrayList<JavaType>()),
        new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(),
        null, null));

		// setter methods for EmployeeRecord
		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
      JavaType methodReturnType = JavaType.VOID_PRIMITIVE;
        // propToGwtSideType.get(propertyName);
      JavaSymbolName methodName = new JavaSymbolName("set" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
      List<JavaType> methodParameterTypes = Collections.<JavaType>singletonList(propToGwtSideType.get(propertyName));
      List<JavaSymbolName> methodParameterNames = Collections.<JavaSymbolName>singletonList(propertyName);

      List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
      MethodMetadata methodMetadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, annotations, null, null);
      methods.add(methodMetadata);
    }


		this.record = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void addReference(VelocityContext ctx, MirrorType type) {
		List<String> imports = asList(ctx.get("imports"));
    addImport(imports, type);
		Map<String, String> map = asMap(ctx.get("entity"));
    Map<String, String> eMap = map;
		eMap.put(type.getVelocityName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private void addReference(VelocityContext ctx, SharedType type) {
		List<String> imports = asList(ctx.get("imports"));
    addImport(imports, type);
		Map<String, String> map = asMap(ctx.get("shared"));
    Map<String, String> sMap = map;
		sMap.put(type.getVelocityName(), getDestinationJavaType(type).getSimpleTypeName());
	}

	private void addImport(List<String> imports, SharedType type) {
		imports.add(getDestinationJavaType(type).getFullyQualifiedTypeName());
	}

	private void addImport(List<String> imports, MirrorType type) {
		imports.add(getDestinationJavaType(type).getFullyQualifiedTypeName());
	}
	
  @SuppressWarnings("unchecked")
  private <T> List<T> asList(Object object) {
    return (List<T>) object;
  }
  
  @SuppressWarnings("unchecked")
  private <K, V> Map<K, V> asMap(Object object) {
    return (Map<K, V>) object;
  }
  
	private void buildEditActivity() {
		try {
			MirrorType type = MirrorType.EDIT_ACTIVITY;
			VelocityContext ctx = buildContext(MirrorType.EDIT_ACTIVITY);
			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			addReference(ctx, MirrorType.EDIT_VIEW);

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildDetailsActivity() {
		try {
			MirrorType type = MirrorType.DETAIL_ACTIVITY;

			VelocityContext ctx = buildContext(MirrorType.DETAIL_ACTIVITY);

			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			addReference(ctx, MirrorType.DETAILS_VIEW);

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildListActivity() {
		try {
			MirrorType type = MirrorType.LIST_ACTIVITY;

			VelocityContext ctx = buildContext(MirrorType.LIST_ACTIVITY);

			addReference(ctx, SharedType.APP_REQUEST_FACTORY);
			addReference(ctx, MirrorType.LIST_VIEW);
			addReference(ctx, MirrorType.RECORD_CHANGED);
			addReference(ctx, MirrorType.CHANGED_HANDLER);

			writeWithTemplate(type, ctx, TemplateResourceLoader.TEMPLATE_DIR + type.getVelocityTemplate());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildListView() {
		try {
			writeWithTemplate(MirrorType.LIST_VIEW, "org/springframework/roo/addon/gwt/templates/ListView.vm");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void writeWithTemplate(MirrorType destType, String templateFile) throws Exception {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, buildContext(destType), templateFile);
	}

	private void writeWithHapaxTemplate(MirrorType destType, String baseDir, String templateFile) throws Exception {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithHapaxTemplate(destFile, buildDataDictionary(destType), baseDir, templateFile);
	}

	private void writeWithTemplate(MirrorType destType, VelocityContext context, String templateFile) throws Exception {
		String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";
		writeWithTemplate(destFile, context, templateFile);
	}

	private void writeWithTemplate(String destFile, VelocityContext context, String templateFile) throws Exception {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("resource.loader", "mine");
		engine.setProperty("mine.resource.loader.instance", new TemplateResourceLoader());

		StringWriter sw = new StringWriter();
		engine.getTemplate(templateFile).merge(context, sw);
		write(destFile, sw.toString(), fileManager);
	}

	private void writeWithHapaxTemplate(String destFile, TemplateDataDictionary dataDictionary, String baseDir, String templateFile) throws Exception {
		TemplateLoader templateLoader = GwtTemplateLoader.create(baseDir);
		Template template = templateLoader.getTemplate(templateFile);
		write(destFile, template.renderToString(dataDictionary), fileManager);
	}

	public static class Property {
		private String name;
		private String getter;
		private String setter;
		private JavaType type;

		public Property(String getter, String name, String setter) {
			this.getter = getter;
			this.name = name;
			this.setter = setter;
		}

		public Property(String getter, String name, String setting, JavaType returnType) {
			this(getter, name, setting);
			this.type = returnType;
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

		public void setGetter(String getter) {
			this.getter = getter;
		}

		public boolean isNonString() {
			return type != null && type.equals(new JavaType("java.util.Date"));
		}

		public String getBinder() {
			if (type.equals(JavaType.DOUBLE_OBJECT))
				return "app:DoubleBox";
			if (type.equals(JavaType.LONG_OBJECT))
				return "app:LongBox";
			if (type.equals(JavaType.INT_OBJECT))
				return "app:IntegerBox";
			return isNonString() ? "d:DateBox" : "g:TextBox";
		}

		public String getEditor() {
			if (type.equals(JavaType.DOUBLE_OBJECT))
				return "DoubleBox";
			if (type.equals(JavaType.LONG_OBJECT))
				return "LongBox";
			if (type.equals(JavaType.INT_OBJECT))
				return "IntegerBox";

			return isNonString() ? "DateBox" : "TextBox";
		}

		public String getFormatter() {
			return isNonString() ? "DateTimeFormat.getShortDateFormat().format(" : "String.valueOf(";
		}

		public String getRenderer() {
			return isNonString() ? "new DateTimeFormatRenderer(DateTimeFormat.getShortDateFormat())" : "new AbstractRenderer<" + getType() + ">() {\n      public String render(" + getType() + " obj) {\n        return String.valueOf(obj);\n      }    \n}";
		}

		public String getReadableName() {
			return new JavaSymbolName(name).getReadableSymbolName();
		}
		
		public String toString() {
			return new StringBuilder(getName()).append(".setInnerText(").append(getFormatter()).append("record.").append(getGetter()).append("()));").toString();
		}
	}

	private VelocityContext buildContext(MirrorType destType) {
		JavaType javaType = getDestinationJavaType(destType);
		String clazz = javaType.getSimpleTypeName();
		JavaType recordType = getDestinationJavaType(MirrorType.RECORD);

		VelocityContext context = new VelocityContext();
		context.put("shared", new HashMap<String, String>());
		HashMap<String, Object> eMap = new HashMap<String, Object>();
		context.put("entity", eMap);
		context.put("className", clazz);
		context.put("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		ArrayList<String> imports = new ArrayList<String>();
		imports.add(recordType.getFullyQualifiedTypeName());
		context.put("imports", imports);
		eMap.put("name", governorTypeDetails.getName().getSimpleTypeName());
		eMap.put("pluralName", entityMetadata.getPlural());
		eMap.put("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
		eMap.put("record", recordType.getSimpleTypeName());
		eMap.put("pluralName", entityMetadata.getPlural());

		ArrayList<String> fieldNames = new ArrayList<String>();
		for (FieldMetadata f : record.getDeclaredFields()) {
			if (f.getFieldName().getSymbolName().equals("TOKEN")) {
				continue;
			}
			fieldNames.add(f.getFieldName().getSymbolName());
		}
		eMap.put("fields", fieldNames);

		ArrayList<Property> props = new ArrayList<Property>();
		for (MethodMetadata f : record.getDeclaredMethods()) {
			if (!f.getMethodName().getSymbolName().startsWith("get")) {
				continue;
			}
			String getter = f.getMethodName().getSymbolName();
			props.add(new Property(getter, StringUtils.uncapitalize(getter.substring(3)), "set" + getter.substring(3), f.getReturnType()));
		}
		eMap.put("properties", props);
		return context;
	}

	private TemplateDataDictionary buildDataDictionary(MirrorType destType) {
		JavaType javaType = getDestinationJavaType(destType);
		String clazz = javaType.getSimpleTypeName();
		JavaType recordType = getDestinationJavaType(MirrorType.RECORD);

		TemplateDataDictionary dataDictionary = TemplateDictionary.create();
		HashMap<String, Object> eMap = new HashMap<String, Object>();
		dataDictionary.setVariable("className", clazz);
		dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
		ArrayList<String> imports = new ArrayList<String>();
		imports.add(recordType.getFullyQualifiedTypeName());
		dataDictionary.addSection("imports").setVariable("import", recordType.getFullyQualifiedTypeName());
		dataDictionary.setVariable("name", governorTypeDetails.getName().getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
		dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
		dataDictionary.setVariable("record", recordType.getSimpleTypeName());
		dataDictionary.setVariable("pluralName", entityMetadata.getPlural());

		ArrayList<String> fieldNames = new ArrayList<String>();
		for (FieldMetadata f : record.getDeclaredFields()) {
			if (f.getFieldName().getSymbolName().equals("TOKEN")) {
				continue;
			}
			dataDictionary.addSection("entityFields").setVariable("field", f.getFieldName().getSymbolName());
			fieldNames.add(f.getFieldName().getSymbolName());
		}
		eMap.put("fields", fieldNames);

		ArrayList<Property> props = new ArrayList<Property>();
		for (MethodMetadata f : record.getDeclaredMethods()) {
			if (!f.getMethodName().getSymbolName().startsWith("get")) {
				continue;
			}
			String getter = f.getMethodName().getSymbolName();
			Property property = new Property(getter, StringUtils.uncapitalize(getter.substring(3)), "set" + getter.substring(3), f.getReturnType());
			dataDictionary.addSection("entityProperties").setVariable("prop", property.toString());

		}
		eMap.put("properties", props);
		return dataDictionary;
	}

	private void buildListViewUiXml() {
		MirrorType dType = MirrorType.LIST_VIEW;
		String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(dType).getSimpleTypeName() + ".ui.xml";
		try {
			writeWithTemplate(destFile, buildContext(dType), "org/springframework/roo/addon/gwt/templates/ListViewUiXml.vm");
		} catch (Exception e) {
			throw new IllegalStateException(e); // To change body of catch statement use File | Settings | File Templates.
		}
	}

	private void buildDetailsViewHapax() {
		try {
			writeWithHapaxTemplate(MirrorType.DETAILS_VIEW, "org/springframework/roo/addon/gwt/templates/", "DetailsView");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildDetailsViewUiXml() {
		MirrorType dType = MirrorType.DETAILS_VIEW;
		String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(dType).getSimpleTypeName() + ".ui.xml";
		try {
			writeWithTemplate(destFile, buildContext(dType), "org/springframework/roo/addon/gwt/templates/DetailsViewUiXml.vm");
		} catch (Exception e) {
			throw new IllegalStateException(e); // To change body of catch statement use File | Settings | File Templates.
		}
	}

	private void buildEditView() {
		MirrorType dType = MirrorType.EDIT_VIEW;
		try {
			writeWithTemplate(dType, "org/springframework/roo/addon/gwt/templates/EditView.vm");
		} catch (Exception e) {
			throw new IllegalStateException(e); // To change body of catch statement use File | Settings | File Templates.
		}
	}

	private void buildEditViewUiXml() {
		MirrorType dType = MirrorType.EDIT_VIEW;
		String destFile = dType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(dType).getSimpleTypeName() + ".ui.xml";
		try {
			writeWithTemplate(destFile, buildContext(dType), "org/springframework/roo/addon/gwt/templates/EditViewUiXml.vm");
		} catch (Exception e) {
			throw new IllegalStateException(e); // To change body of catch statement use File | Settings | File Templates.
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

		// remove(EmployeeRecord record) and persist(EmployeeRecord record) methods.
		JavaType methodReturnType = new JavaType(
        "com.google.gwt.requestfactory.shared.RequestObject", 0,
        DataType.TYPE, null, Collections.singletonList(JavaType.VOID_OBJECT));
    for (MethodMetadata metadata : new MethodMetadata[] {
        entityMetadata.getRemoveMethod(), entityMetadata.getPersistMethod()}) {
      List<AnnotatedJavaType> parameterTypes = Collections.singletonList(new AnnotatedJavaType(
          getDestinationJavaType(MirrorType.RECORD), null));
      List<JavaSymbolName> parameterNames = Collections.singletonList(new JavaSymbolName(
          "record"));
      List<AnnotationMetadata> annotations = Collections.singletonList((AnnotationMetadata) new DefaultAnnotationMetadata(
          new JavaType("com.google.gwt.requestfactory.shared.Instance"), Collections.<AnnotationAttributeValue<?>>emptyList()));
      MethodMetadata method1Metadata = new DefaultMethodMetadata(
          destinationMetadataId, Modifier.ABSTRACT, metadata.getMethodName(),
          methodReturnType, parameterTypes, parameterNames, annotations, null, null);
      methods.add(method1Metadata);
    }

		this.request = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildRequestMethod(String destinationMetadataId, List<MethodMetadata> methods, MethodMetadata methodMetaData) {
		// com.google.gwt.requestfactory.shared.EntityListRequest<EmployeeKey> findAllEmployees();
		JavaSymbolName method1Name = methodMetaData.getMethodName();
		List<JavaType> method1ReturnTypeArgs0 = new ArrayList<JavaType>();
		boolean isList = methodMetaData.getReturnType().getFullyQualifiedTypeName().equals("java.util.List");
		method1ReturnTypeArgs0.add(method1Name.getSymbolName().startsWith("count") ? JavaType.LONG_OBJECT : getDestinationJavaType(MirrorType.RECORD));
		JavaType method1ReturnType = new JavaType(method1Name.getSymbolName().startsWith("count") ? "com.google.gwt.requestfactory.shared.RequestObject" : (isList ? "com.google.gwt.requestfactory.shared.RecordListRequest" : "com.google.gwt.requestfactory.shared.RecordRequest"), 0, DataType.TYPE, null, method1ReturnTypeArgs0);
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
			JavaType propRef = new JavaType("com.google.gwt.valuestore.shared.PropertyReference", 0, DataType.TYPE, null, typeParams);
			method1ParameterTypes.add(jtype.isPrimitive() ? jtype : propRef);
		}

		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, null, null, null);
		methods.add(method1Metadata);
	}

	class ExportedMethod {
		JavaSymbolName operationName; // Mandatory
		JavaSymbolName methodName; // Mandatory
		JavaType returns; // Mandatory
		List<AnnotatedJavaType> args; // Mandatory, but can be empty
		boolean isList;
	}

	private String getOnChangeMethod() {
		return "on" + governorTypeDetails.getName().getSimpleTypeName() + "Changed";
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

	public ClassOrInterfaceTypeDetails getChanged() {
		return recordChanged;
	}

	public ClassOrInterfaceTypeDetails getChangeHandler() {
		return changeHandler;
	}

	public ClassOrInterfaceTypeDetails getKey() {
		return record;
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
