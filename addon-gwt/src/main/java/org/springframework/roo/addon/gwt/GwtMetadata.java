package org.springframework.roo.addon.gwt;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for GWT.
 * 
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 *
 */
public class GwtMetadata extends AbstractMetadataItem {
	private static final String PROVIDES_TYPE_STRING = GwtMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private MethodMetadata findAllMethod;

	private MirrorTypeNamingStrategy mirrorTypeNamingStrategy;
	private ProjectMetadata projectMetadata;
	private ClassOrInterfaceTypeDetails governorTypeDetails;
	private Path mirrorTypePath;
	private ClassOrInterfaceTypeDetails recordChanged;
	private ClassOrInterfaceTypeDetails changeHandler;
	private ClassOrInterfaceTypeDetails request;
	private ClassOrInterfaceTypeDetails requestServerSideOperations;
	private ClassOrInterfaceTypeDetails findAllRequester;

	private ClassOrInterfaceTypeDetails record;
	private JavaSymbolName idPropertyName;
	private JavaSymbolName versionPropertyName;
	private boolean versionIntegerOnServerSide = true;
	private boolean idLongOnServerSide = false;

	private ClassOrInterfaceTypeDetails details;
	private ClassOrInterfaceTypeDetails listView;

	GwtMetadata(String identifier, MirrorTypeNamingStrategy mirrorTypeNamingStrategy, ProjectMetadata projectMetadata, ClassOrInterfaceTypeDetails governorTypeDetails, Path mirrorTypePath, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata) {
		super(identifier);
		this.mirrorTypeNamingStrategy = mirrorTypeNamingStrategy;
		this.projectMetadata = projectMetadata;
		this.governorTypeDetails = governorTypeDetails;
		this.mirrorTypePath = mirrorTypePath;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;

		// We know GwtMetadataProvider already took care of all the necessary checks. So we can just re-create fresh representations of the types we're responsible for
		resolveEntityInformation();
		buildRecordChanged();
		buildChangeHandler();
		buildRecord();
		buildDetailsBuilder();
		buildListView();
		buildRequest();
		buildRequestServerSideOperations();
		buildFindAllRequester();
	}

	public List<ClassOrInterfaceTypeDetails> getAllTypes() {
		List<ClassOrInterfaceTypeDetails> result = new ArrayList<ClassOrInterfaceTypeDetails>();
		result.add(recordChanged);
		result.add(changeHandler);
		result.add(record);
		result.add(details);
		result.add(listView);
		result.add(request);
		result.add(requestServerSideOperations);
		result.add(findAllRequester);
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

		// public EmployeeRecordChanged(com.springsource.extrack.gwt.request.EmployeeRecord record) {
		// super(record);
		// }
		List<JavaType> constructorParameterTypes = new ArrayList<JavaType>();
		constructorParameterTypes.add(getDestinationJavaType(MirrorType.RECORD));
		List<JavaSymbolName> constructorParameterNames = new ArrayList<JavaSymbolName>();
		constructorParameterNames.add(new JavaSymbolName("record"));
		InvocableMemberBodyBuilder constructorBodyBuilder = new InvocableMemberBodyBuilder();
		constructorBodyBuilder.appendFormalLine("super(record);");
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
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

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
				boolean isDomainObject = !(returnType.equals(JavaType.BOOLEAN_OBJECT) || returnType.equals(JavaType.INT_OBJECT) || returnType.equals(JavaType.LONG_OBJECT) || returnType.equals(JavaType.STRING_OBJECT) || returnType.equals(new JavaType("java.util.Date")));
				if (isDomainObject) {
					gwtSideType = getDestinationJavaType(returnType, MirrorType.RECORD);
				} else {
					gwtSideType = returnType;
					// Handle the identifier special case
					if (idPropertyName.equals(propertyName) && idLongOnServerSide) {
						gwtSideType = JavaType.STRING_OBJECT;
					}
					// Handle the version special case
					if (versionPropertyName.equals(propertyName) && versionIntegerOnServerSide) {
						gwtSideType = JavaType.STRING_OBJECT;
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

		for (JavaSymbolName propertyName : propToGwtSideType.keySet()) {
			JavaSymbolName fieldName = propertyName;
			List<JavaType> fieldArgs = new ArrayList<JavaType>();
			fieldArgs.add(propToGwtSideType.get(propertyName));

			JavaType fieldType = new JavaType(propToWrapperType.get(propertyName).getFullyQualifiedTypeName(), 0, DataType.TYPE, null, fieldArgs);
			String fieldInitializer = "new " + fieldType + "(\"" + propertyName.getSymbolName() + "\", " + propToGwtSideType.get(propertyName).getFullyQualifiedTypeName() + ".class)";
			FieldMetadata fieldMetadata = new DefaultFieldMetadata(destinationMetadataId, Modifier.INTERFACE, fieldName, fieldType, fieldInitializer, null);
			fields.add(fieldMetadata);
		}

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

		this.record = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildDetailsBuilder() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.DETAILS_BUILDER);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// static void append(StringBuilder list, EmployeeRecord record) {
		// list.append("<div>");
		// list.append("<label>").append("User Name: ").append("</label>");
		// list.append("<span>").append(record.getUserName()).append("</span>");
		// list.append("</div>");
		// }
		JavaSymbolName methodName = new JavaSymbolName("append");
		JavaType methodReturnType = JavaType.VOID_PRIMITIVE;
		List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
		methodParameterTypes.add(new JavaType("java.lang.StringBuilder"));
		methodParameterTypes.add(getDestinationJavaType(MirrorType.RECORD));
		List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();
		methodParameterNames.add(new JavaSymbolName("list"));
		methodParameterNames.add(new JavaSymbolName("record"));
		InvocableMemberBodyBuilder methodBodyBuilder = new InvocableMemberBodyBuilder();

		for (MethodMetadata m : record.getDeclaredMethods()) {
			if (Modifier.isStatic(m.getModifier()) || !m.getMethodName().getSymbolName().startsWith("get")) {
				// Skip this method
				continue;
			}
			String label = BeanInfoMetadata.getPropertyNameForJavaBeanMethod(m).getReadableSymbolName();
			String methodInvocation = m.getMethodName() + "()";
			methodBodyBuilder.appendFormalLine("list.append(\"<div>\");");
			methodBodyBuilder.appendFormalLine("list.append(\"<label>\").append(\"" + label + ": \").append(\"</label>\");");
			methodBodyBuilder.appendFormalLine("list.append(\"<span>\").append(record." + methodInvocation + ").append(\"</span>\");");
			methodBodyBuilder.appendFormalLine("list.append(\"</div>\");");
		}

		MethodMetadata methodMetadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.STATIC, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, null, null, methodBodyBuilder.getOutput());
		methods.add(methodMetadata);

		this.details = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.FINAL, PhysicalTypeCategory.CLASS, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildListView() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.LIST_VIEW);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// extends com.google.gwt.valuestore.client.ValuesListViewTable<EmployeeRecord>
		List<JavaType> extendsTypeParams = new ArrayList<JavaType>();
		extendsTypeParams.add(getDestinationJavaType(MirrorType.RECORD));
		extendsTypes.add(new JavaType("com.google.gwt.valuestore.client.ValuesListViewTable", 0, DataType.TYPE, null, extendsTypeParams));

		// public EmployeeListView(String headingMessage, ApplicationPlaces places, ApplicationRequestFactory requests) {
		// super(headingMessage, getColumns(places), getHeaders());
		// }
		List<JavaType> constructorParameterTypes = new ArrayList<JavaType>();
		constructorParameterTypes.add(JavaType.STRING_OBJECT);
		constructorParameterTypes.add(getDestinationJavaType(SharedType.APP_PLACES));
		constructorParameterTypes.add(getDestinationJavaType(SharedType.APP_REQUEST_FACTORY));
		List<JavaSymbolName> constructorParameterNames = new ArrayList<JavaSymbolName>();
		constructorParameterNames.add(new JavaSymbolName("headingMessage"));
		constructorParameterNames.add(new JavaSymbolName("places"));
		constructorParameterNames.add(new JavaSymbolName("requests"));
		InvocableMemberBodyBuilder constructorBodyBuilder = new InvocableMemberBodyBuilder();
		constructorBodyBuilder.appendFormalLine("super(headingMessage, getColumns(places), getHeaders());");
		ConstructorMetadata constructorMetadata = new DefaultConstructorMetadata(destinationMetadataId, Modifier.PUBLIC, AnnotatedJavaType.convertFromJavaTypes(constructorParameterTypes), constructorParameterNames, null, constructorBodyBuilder.getOutput());
		constructors.add(constructorMetadata);

		// private static List<Header<?>> getHeaders() {
		// List<Header<?>> headers = new ArrayList<Header<?>>();
		// for (final Property<?> property : getProps()) {
		// headers.add(new TextHeader(property.getName()));
		// }
		// return headers;
		// }
		JavaSymbolName method1Name = new JavaSymbolName("getHeaders");
		List<JavaType> method1ReturnParams = new ArrayList<JavaType>();
		List<JavaType> method1ReturnParam1 = new ArrayList<JavaType>();
		method1ReturnParam1.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		method1ReturnParams.add(new JavaType("com.google.gwt.bikeshed.list.client.Header", 0, DataType.TYPE, null, method1ReturnParam1));
		JavaType method1ReturnType = new JavaType("java.util.List", 0, DataType.TYPE, null, method1ReturnParams);
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		InvocableMemberBodyBuilder method1BodyBuilder = new InvocableMemberBodyBuilder();
		method1BodyBuilder.appendFormalLine("List<com.google.gwt.bikeshed.list.client.Header<?>> headers = new java.util.ArrayList<com.google.gwt.bikeshed.list.client.Header<?>>();");
		method1BodyBuilder.appendFormalLine("for (final com.google.gwt.valuestore.shared.Property<?> property : getProps()) {");
		method1BodyBuilder.indent();
		method1BodyBuilder.appendFormalLine("headers.add(new com.google.gwt.bikeshed.list.client.TextHeader(property.getName()));");
		method1BodyBuilder.indentRemove();
		method1BodyBuilder.appendFormalLine("}");
		method1BodyBuilder.appendFormalLine("return headers;");
		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PRIVATE + Modifier.STATIC, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, null, null, method1BodyBuilder.getOutput());
		methods.add(method1Metadata);

		// public Collection<Property<?>> getProperties() {
		// return getProps();
		// }
		JavaSymbolName method2Name = new JavaSymbolName("getProperties");
		List<JavaType> method2ReturnParams = new ArrayList<JavaType>();
		List<JavaType> method2ReturnParam1 = new ArrayList<JavaType>();
		method2ReturnParam1.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		method2ReturnParams.add(new JavaType("com.google.gwt.valuestore.shared.Property", 0, DataType.TYPE, null, method2ReturnParam1));
		JavaType method2ReturnType = new JavaType("java.util.Collection", 0, DataType.TYPE, null, method2ReturnParams);
		List<JavaType> method2ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method2ParameterNames = new ArrayList<JavaSymbolName>();
		InvocableMemberBodyBuilder method2BodyBuilder = new InvocableMemberBodyBuilder();
		method2BodyBuilder.appendFormalLine("return getProps();");
		MethodMetadata method2Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method2Name, method2ReturnType, AnnotatedJavaType.convertFromJavaTypes(method2ParameterTypes), method2ParameterNames, null, null, method2BodyBuilder.getOutput());
		methods.add(method2Metadata);

		// public static Collection<com.google.gwt.valuestore.shared.Property<?>> getProps() {
		// List<Property<?>> properties = new ArrayList<Property<?>>();
		// properties.add(EmployeeRecord.userName);
		// properties.add(EmployeeRecord.displayName);
		// return properties;
		// }
		JavaSymbolName method3Name = new JavaSymbolName("getProps");
		List<JavaType> method3ReturnParams = new ArrayList<JavaType>();
		List<JavaType> method3ReturnParam1 = new ArrayList<JavaType>();
		method3ReturnParam1.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		method3ReturnParams.add(new JavaType("com.google.gwt.valuestore.shared.Property", 0, DataType.TYPE, null, method3ReturnParam1));
		JavaType method3ReturnType = new JavaType("java.util.Collection", 0, DataType.TYPE, null, method3ReturnParams);
		List<JavaType> method3ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method3ParameterNames = new ArrayList<JavaSymbolName>();
		InvocableMemberBodyBuilder method3BodyBuilder = new InvocableMemberBodyBuilder();
		method3BodyBuilder.appendFormalLine("java.util.List<com.google.gwt.valuestore.shared.Property<?>> properties = new java.util.ArrayList<com.google.gwt.valuestore.shared.Property<?>>();");
		for (FieldMetadata f : record.getDeclaredFields()) {
			if (!f.getFieldType().getParameters().get(0).equals(JavaType.STRING_OBJECT)) {
				// We only support strings for now
				continue;
			}
			String field = getDestinationJavaType(MirrorType.RECORD).getSimpleTypeName() + "." + f.getFieldName().getSymbolName();
			method3BodyBuilder.appendFormalLine("properties.add(" + field + ");");
		}
		method3BodyBuilder.appendFormalLine("return properties;");
		MethodMetadata method3Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PRIVATE + Modifier.STATIC, method3Name, method3ReturnType, AnnotatedJavaType.convertFromJavaTypes(method3ParameterTypes), method3ParameterNames, null, null, method3BodyBuilder.getOutput());
		methods.add(method3Metadata);

		// private static List<Column<EmployeeRecord, ?, ?>> getColumns(ApplicationPlaces places) {
		JavaSymbolName method4Name = new JavaSymbolName("getColumns");
		List<JavaType> method4ReturnParams = new ArrayList<JavaType>();
		List<JavaType> method4ReturnParam1 = new ArrayList<JavaType>();
		method4ReturnParam1.add(getDestinationJavaType(MirrorType.RECORD));
		method4ReturnParam1.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		method4ReturnParam1.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		method4ReturnParams.add(new JavaType("com.google.gwt.bikeshed.list.client.Column", 0, DataType.TYPE, null, method4ReturnParam1));
		JavaType method4ReturnType = new JavaType("java.util.List", 0, DataType.TYPE, null, method4ReturnParams);
		List<JavaType> method4ParameterTypes = new ArrayList<JavaType>();
		method4ParameterTypes.add(getDestinationJavaType(SharedType.APP_PLACES));
		List<JavaSymbolName> method4ParameterNames = new ArrayList<JavaSymbolName>();
		method4ParameterNames.add(new JavaSymbolName("places"));
		String entityRecord = getDestinationJavaType(MirrorType.RECORD).getSimpleTypeName();
		InvocableMemberBodyBuilder method4BodyBuilder = new InvocableMemberBodyBuilder();
		method4BodyBuilder.appendFormalLine("final ApplicationPlaces p = places;");
		JavaType method4ArrayList = new JavaType("java.util.ArrayList", 0, DataType.TYPE, null, method4ReturnParams);
		method4BodyBuilder.appendFormalLine(method4ReturnType.toString() + " columns = new " + method4ArrayList.toString() + "();");
		for (MethodMetadata m : record.getDeclaredMethods()) {
			if (Modifier.isStatic(m.getModifier()) || !m.getMethodName().getSymbolName().startsWith("get")) {
				// skip this method
				continue;
			}
			if (!m.getReturnType().equals(JavaType.STRING_OBJECT)) {
				// we only support strings for now
				continue;
			}
			method4BodyBuilder.appendFormalLine("columns.add(new com.google.gwt.bikeshed.list.client.TextColumn<" + entityRecord + ">() {");
			method4BodyBuilder.indent();
			method4BodyBuilder.appendFormalLine("public String getValue(" + entityRecord + " object) {");
			method4BodyBuilder.indent();
			String methodInvocation = m.getMethodName() + "()";
			method4BodyBuilder.appendFormalLine("return object." + methodInvocation + ";");
			method4BodyBuilder.indentRemove();
			method4BodyBuilder.appendFormalLine("}");
			method4BodyBuilder.indentRemove();
			method4BodyBuilder.appendFormalLine("});");
		}
		method4BodyBuilder.appendFormalLine("columns.add(new com.google.gwt.bikeshed.list.client.IdentityColumn<" + entityRecord + ">(new com.google.gwt.bikeshed.cells.client.ActionCell<" + entityRecord + ">(\"Show\", p.<" + entityRecord + "> getDetailsGofer())));");
		method4BodyBuilder.appendFormalLine("return columns;");
		MethodMetadata method4Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PRIVATE + Modifier.STATIC, method4Name, method4ReturnType, AnnotatedJavaType.convertFromJavaTypes(method4ParameterTypes), method4ParameterNames, null, null, method4BodyBuilder.getOutput());
		methods.add(method4Metadata);

		this.listView = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.FINAL, PhysicalTypeCategory.CLASS, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildRequest() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.REQUEST);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		// @com.google.gwt.requestfactory.shared.ServerOperation("FIND_ALL_EMPLOYEES")
		// com.google.gwt.requestfactory.shared.EntityListRequest<EmployeeKey> findAllEmployees();
		JavaSymbolName method1Name = findAllMethod.getMethodName();
		List<JavaType> method1ReturnTypeArgs0 = new ArrayList<JavaType>();
		method1ReturnTypeArgs0.add(getDestinationJavaType(MirrorType.RECORD));
		JavaType method1ReturnType = new JavaType("com.google.gwt.requestfactory.shared.EntityListRequest", 0, DataType.TYPE, null, method1ReturnTypeArgs0);
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		List<AnnotationMetadata> method1Annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> method1AnnotationAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		method1AnnotationAttrs.add(new StringAttributeValue(new JavaSymbolName("value"), computeServerOperationName(findAllMethod)));
		method1Annotations.add(new DefaultAnnotationMetadata(new JavaType("com.google.gwt.requestfactory.shared.ServerOperation"), method1AnnotationAttrs));
		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.ABSTRACT, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, method1Annotations, null, null);
		methods.add(method1Metadata);

		this.request = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.INTERFACE, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	private void buildRequestServerSideOperations() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.REQUEST_SERVER_SIDE_OPERATIONS);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		implementsTypes.add(new JavaType("com.google.gwt.requestfactory.shared.RequestFactory.RequestDefinition"));

		// public String getDomainClassName()
		JavaSymbolName method1Name = new JavaSymbolName("getDomainClassName");
		JavaType method1ReturnType = JavaType.STRING_OBJECT;
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		List<AnnotationMetadata> method1Annotations = new ArrayList<AnnotationMetadata>();
		InvocableMemberBodyBuilder method1BodyBuilder = new InvocableMemberBodyBuilder();
		method1BodyBuilder.appendFormalLine("return \"" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "\";");
		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, method1Annotations, null, method1BodyBuilder.getOutput());
		methods.add(method1Metadata);

		// To avoid needing to add extra features to our Java Parser integration just for complex enums, I'm avoiding calling constructors from an enum constant name.
		// This means we have to locate what we want to export in advance, and then write out the file properly.

		// Locate the methods we want to export
		List<ExportedMethod> toExport = new ArrayList<ExportedMethod>();
		ExportedMethod e1 = new ExportedMethod();
		e1.operationName = new JavaSymbolName(computeServerOperationName(findAllMethod));
		e1.methodName = findAllMethod.getMethodName();
		e1.returns = getDestinationJavaType(MirrorType.RECORD);
		e1.args = findAllMethod.getParameterTypes();
		toExport.add(e1);

		// Add the enums themselves
		for (ExportedMethod exported : toExport) {
			enumConstants.add(exported.operationName);
		}

		// public String getDomainMethodName() method
		JavaSymbolName method2Name = new JavaSymbolName("getDomainMethodName");
		JavaType method2ReturnType = JavaType.STRING_OBJECT;
		List<JavaType> method2ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method2ParameterNames = new ArrayList<JavaSymbolName>();
		List<AnnotationMetadata> method2Annotations = new ArrayList<AnnotationMetadata>();
		InvocableMemberBodyBuilder method2BodyBuilder = new InvocableMemberBodyBuilder();
		method2BodyBuilder.appendFormalLine("switch (this) {");
		method2BodyBuilder.indent();
		for (ExportedMethod exported : toExport) {
			method2BodyBuilder.appendFormalLine("case " + exported.operationName + ": return \"" + exported.methodName + "\";");
		}
		method2BodyBuilder.appendFormalLine("default: throw new IllegalStateException();");
		method2BodyBuilder.indentRemove();
		method2BodyBuilder.appendFormalLine("}");
		MethodMetadata method2Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method2Name, method2ReturnType, AnnotatedJavaType.convertFromJavaTypes(method2ParameterTypes), method2ParameterNames, method2Annotations, null, method2BodyBuilder.getOutput());
		methods.add(method2Metadata);

		// public Class<? extends Record> getReturnType() method
		JavaSymbolName method3Name = new JavaSymbolName("getReturnType");
		List<JavaType> method3ReturnTypeParams = new ArrayList<JavaType>();
		List<JavaType> method3ReturnTypeValueKey = new ArrayList<JavaType>();
		method3ReturnTypeValueKey.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		method3ReturnTypeParams.add(new JavaType("com.google.gwt.valuestore.shared.Record", 0, DataType.TYPE, JavaType.WILDCARD_EXTENDS, null));
		JavaType method3ReturnType = new JavaType("java.lang.Class", 0, DataType.TYPE, null, method3ReturnTypeParams);
		List<JavaType> method3ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method3ParameterNames = new ArrayList<JavaSymbolName>();
		List<AnnotationMetadata> method3Annotations = new ArrayList<AnnotationMetadata>();
		InvocableMemberBodyBuilder method3BodyBuilder = new InvocableMemberBodyBuilder();
		method3BodyBuilder.appendFormalLine("switch (this) {");
		method3BodyBuilder.indent();
		for (ExportedMethod exported : toExport) {
			method3BodyBuilder.appendFormalLine("case " + exported.operationName + ": return " + exported.returns.getFullyQualifiedTypeName() + ".class;");
		}
		method3BodyBuilder.appendFormalLine("default: throw new IllegalStateException();");
		method3BodyBuilder.indentRemove();
		method3BodyBuilder.appendFormalLine("}");
		MethodMetadata method3Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method3Name, method3ReturnType, AnnotatedJavaType.convertFromJavaTypes(method3ParameterTypes), method3ParameterNames, method3Annotations, null, method3BodyBuilder.getOutput());
		methods.add(method3Metadata);

		// public Class<?>[] getParameterTypes() method
		JavaSymbolName method4Name = new JavaSymbolName("getParameterTypes");
		List<JavaType> method4ReturnTypeParams = new ArrayList<JavaType>();
		method4ReturnTypeParams.add(new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null));
		JavaType method4ReturnType = new JavaType("java.lang.Class", 1, DataType.TYPE, null, method4ReturnTypeParams);
		List<JavaType> method4ParameterTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> method4ParameterNames = new ArrayList<JavaSymbolName>();
		List<AnnotationMetadata> method4Annotations = new ArrayList<AnnotationMetadata>();
		InvocableMemberBodyBuilder method4BodyBuilder = new InvocableMemberBodyBuilder();
		method4BodyBuilder.appendFormalLine("switch (this) {");
		method4BodyBuilder.indent();
		for (ExportedMethod exported : toExport) {
			String text = "null";
			if (exported.args.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("{ ");
				boolean firstElement = true;
				for (AnnotatedJavaType arg : exported.args) {
					if (text != null) {
						if (!firstElement) {
							sb.append(", ");
							firstElement = false;
						}
						text = text + arg.getJavaType().getFullyQualifiedTypeName() + ".class";
					}
				}
				sb.append(" }");
				text = sb.toString();
			}
			method4BodyBuilder.appendFormalLine("case " + exported.operationName + ": return " + text + ";");
		}
		method4BodyBuilder.appendFormalLine("default: throw new IllegalStateException();");
		method4BodyBuilder.indentRemove();
		method4BodyBuilder.appendFormalLine("}");
		MethodMetadata method4Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method4Name, method4ReturnType, AnnotatedJavaType.convertFromJavaTypes(method4ParameterTypes), method4ParameterNames, method4Annotations, null, method4BodyBuilder.getOutput());
		methods.add(method4Metadata);

		this.requestServerSideOperations = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC, PhysicalTypeCategory.ENUMERATION, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, enumConstants);
	}

	private void buildFindAllRequester() {
		String destinationMetadataId = getDestinationMetadataId(MirrorType.FIND_ALL_REQUESTER);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadata> typeAnnotations = createAnnotations();
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		List<JavaType> implementsTypes = new ArrayList<JavaType>();

		implementsTypes.add(new JavaType("com.google.gwt.valuestore.shared.ValuesListView.Delegate"));

		// private final ValuesListViewTable<EmployeeKey> view;
		List<JavaType> field1Params = new ArrayList<JavaType>();
		field1Params.add(getDestinationJavaType(MirrorType.RECORD));
		JavaType field1Type = new JavaType("com.google.gwt.valuestore.client.ValuesListViewTable", 0, DataType.TYPE, null, field1Params);
		FieldMetadata field1Metadata = new DefaultFieldMetadata(destinationMetadataId, Modifier.PRIVATE + Modifier.FINAL, new JavaSymbolName("view"), field1Type, null, null);
		fields.add(field1Metadata);

		// private final ApplicationRequestFactory requests;
		JavaType field2Type = getDestinationJavaType(SharedType.APP_REQUEST_FACTORY);
		FieldMetadata field2Metadata = new DefaultFieldMetadata(destinationMetadataId, Modifier.PRIVATE + Modifier.FINAL, new JavaSymbolName("requests"), field2Type, null, null);
		fields.add(field2Metadata);

		// public EmployeeFindAllRequester(ApplicationRequestFactory requests, ValuesListViewTable<EmployeeKey> view) {
		// this.view = view;
		// this.requests = requests;
		// }
		List<JavaType> constructorParameterTypes = new ArrayList<JavaType>();
		constructorParameterTypes.add(field2Type);
		constructorParameterTypes.add(field1Type);
		List<JavaSymbolName> constructorParameterNames = new ArrayList<JavaSymbolName>();
		constructorParameterNames.add(new JavaSymbolName("requests"));
		constructorParameterNames.add(new JavaSymbolName("view"));
		InvocableMemberBodyBuilder constructorBodyBuilder = new InvocableMemberBodyBuilder();
		constructorBodyBuilder.appendFormalLine("this.view = view;");
		constructorBodyBuilder.appendFormalLine("this.requests = requests;");
		ConstructorMetadata constructorMetadata = new DefaultConstructorMetadata(destinationMetadataId, Modifier.PUBLIC, AnnotatedJavaType.convertFromJavaTypes(constructorParameterTypes), constructorParameterNames, null, constructorBodyBuilder.getOutput());
		constructors.add(constructorMetadata);

		// public void onRangeChanged(int start, int length) {
		// requests.employeeRequest().findAllEmployees().forProperties(view.getProperties()).to(view).fire();
		// }
		JavaSymbolName method1Name = new JavaSymbolName("onRangeChanged");
		JavaType method1ReturnType = JavaType.VOID_PRIMITIVE;
		List<JavaType> method1ParameterTypes = new ArrayList<JavaType>();
		method1ParameterTypes.add(JavaType.INT_PRIMITIVE);
		method1ParameterTypes.add(JavaType.INT_PRIMITIVE);
		List<JavaSymbolName> method1ParameterNames = new ArrayList<JavaSymbolName>();
		method1ParameterNames.add(new JavaSymbolName("start"));
		method1ParameterNames.add(new JavaSymbolName("length"));
		List<AnnotationMetadata> method1Annotations = new ArrayList<AnnotationMetadata>();
		InvocableMemberBodyBuilder method1BodyBuilder = new InvocableMemberBodyBuilder();
		method1BodyBuilder.appendFormalLine("requests." + getFindAllMethodGwtSize() + ".forProperties(view.getProperties()).to(view).fire();");
		MethodMetadata method1Metadata = new DefaultMethodMetadata(destinationMetadataId, Modifier.PUBLIC, method1Name, method1ReturnType, AnnotatedJavaType.convertFromJavaTypes(method1ParameterTypes), method1ParameterNames, method1Annotations, null, method1BodyBuilder.getOutput());
		methods.add(method1Metadata);

		this.findAllRequester = new DefaultClassOrInterfaceTypeDetails(destinationMetadataId, name, Modifier.PUBLIC + Modifier.FINAL, PhysicalTypeCategory.CLASS, constructors, fields, methods, null, extendsTypes, implementsTypes, typeAnnotations, null);
	}

	class ExportedMethod {
		JavaSymbolName operationName; // mandatory
		JavaSymbolName methodName; // mandatory
		JavaType returns; // mandatory
		List<AnnotatedJavaType> args; // mandatory, but can be empty
	}

	private String computeServerOperationName(MethodMetadata serverMethod) {
		return new JavaSymbolName(governorTypeDetails.getName().getSimpleTypeName()).getReadableSymbolName().toUpperCase().replace(' ', '_') + "___" + serverMethod.getMethodName().getReadableSymbolName().toUpperCase().replace(' ', '_');
	}

	private String getOnChangeMethod() {
		return "on" + governorTypeDetails.getName().getSimpleTypeName() + "Changed";
	}

	private String getFindAllMethodGwtSize() {
		return StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()) + "Request()." + findAllMethod.getMethodName().getSymbolName() + "()";
	}

	public MethodMetadata getFindAllMethodServerSide() {
		return findAllMethod;
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
