package org.springframework.roo.addon.gwt;

import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	private EntityMetadata entityMetadata;
	private MethodMetadata findAllMethod;
	private MethodMetadata findMethod;
	private MethodMetadata countMethod;
	private MethodMetadata findEntriesMethod;

	private Map<GwtType, JavaType> mirrorTypeMap;
	private ClassOrInterfaceTypeDetails governorTypeDetails;
	private Path mirrorTypePath;
	private Map<JavaSymbolName, GwtProxyProperty> orderedProxyFields;
	private Map<JavaType, JavaType> gwtTypeMap;

	public GwtMetadata(String identifier, Map<GwtType, JavaType> mirrorTypeMap, ClassOrInterfaceTypeDetails governorTypeDetails, Path mirrorTypePath, EntityMetadata entityMetadata, Map<JavaSymbolName, GwtProxyProperty> fieldTypeMap, Map<JavaType, JavaType> gwtTypeMap) {
		super(identifier);
		Assert.notNull(governorTypeDetails, "Governor details are required");
		Assert.notNull(mirrorTypeMap, "Mirror Type Map is required");
		Assert.notNull(mirrorTypePath, "Mirror Type Path is required");
		Assert.notNull(governorTypeDetails, "Governor details are required");
		Assert.notNull(entityMetadata, "Entity Metadata is required");
		Assert.notNull(gwtTypeMap, "GWT Type Map is required");
		Assert.notNull(fieldTypeMap, "Field Type Map is required");

		this.governorTypeDetails = governorTypeDetails;
		this.mirrorTypeMap = mirrorTypeMap;
		this.mirrorTypePath = mirrorTypePath;
		this.entityMetadata = entityMetadata;
		this.gwtTypeMap = gwtTypeMap;
		this.orderedProxyFields = fieldTypeMap;

		// We know GwtMetadataProvider already took care of all the necessary checks. So we can just re-create fresh representations of the types we're responsible for
		resolveEntityInformation();
	}

	private void resolveEntityInformation() {
		if (entityMetadata == null || !entityMetadata.isValid()) {
			return;
		}

		// Lookup special fields
		String typeName = governorTypeDetails.getName().getFullyQualifiedTypeName();

		FieldMetadata idField = entityMetadata.getIdentifierField();
		Assert.notNull(idField, "GWT support requires an @Id field for " + typeName);
		JavaSymbolName idPropertyName = idField.getFieldName();
		Assert.isTrue("id".equals(idPropertyName.getSymbolName()), "GWT support requires that an @Id field be named \"id\" (found \"" + idPropertyName + "\") for " + typeName);

		FieldMetadata versionField = entityMetadata.getVersionField();
		Assert.notNull(versionField, "GWT support requires an @Version field for " + typeName);
		JavaSymbolName versionPropertyName = versionField.getFieldName();
		Assert.isTrue("version".equals(versionPropertyName.getSymbolName()), "GWT support requires that an @Version field be named \"version\" (found \"" + versionPropertyName + "\") for " + typeName);

		// Lookup the find and count methods and store them
		findAllMethod = entityMetadata.getFindAllMethod();
		Assert.notNull(findAllMethod, "GWT support requires a findAll method for " + typeName);

		findMethod = entityMetadata.getFindMethod();
		Assert.notNull(findMethod, "GWT support requires a find method for " + typeName);

		findEntriesMethod = entityMetadata.getFindEntriesMethod();
		Assert.notNull(findEntriesMethod, "GWT support requires a findEntries method for " + typeName);

		countMethod = entityMetadata.getCountMethod();
		Assert.notNull(countMethod, "GWT support requires a count method for " + typeName);
	}

	public ClassOrInterfaceTypeDetails buildProxy() {
		String destinationMetadataId = getDestinationMetadataId(GwtType.PROXY);

		// Get the proxy's PhysicalTypeMetaData representation of the on disk proxy
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		// Create a new ClassOrInterfaceTypeDetailsBuilder for the Proxy, will be overridden if the Proxy has already been created
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(destinationMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);

		List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();

		// Add @ProxyForName("com.foo.bar.Foo") annotation
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ProxyForName")));

		// Only add annotations that don't already exist on the target
		for (AnnotationMetadataBuilder annotationBuilder : typeAnnotations) {
			boolean exists = false;
			for (AnnotationMetadataBuilder existingAnnotation : typeDetailsBuilder.getAnnotations()) {
				if (existingAnnotation.getAnnotationType().equals(annotationBuilder.getAnnotationType())) {
					exists = true;
					break;
				}
			}

			if (!exists) {
				typeDetailsBuilder.addAnnotation(annotationBuilder);
			}
		}

		// Only inherit from EntityProxy if extension is not already defined
		if (!typeDetailsBuilder.getExtendsTypes().contains(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"))) {
			typeDetailsBuilder.addExtendsTypes(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"));
		}

		/*
		 * Decide which fields we'll be mapping. Remember the natural ordering for
		 * processing, but order proxy getters alphabetically by name.
		 */

		// Getter methods for proxy
		for (JavaSymbolName propertyName : orderedProxyFields.keySet()) {
			JavaType methodReturnType = orderedProxyFields.get(propertyName).getPropertyType();
			JavaSymbolName methodName = new JavaSymbolName("get" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
			List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder());

			// Only add a method if it isn't already present, this leaves the user defined methods in play
			boolean match = false;
			for (MethodMetadataBuilder builder : typeDetailsBuilder.getDeclaredMethods()) {
				if (GwtUtils.methodBuildersEqual(methodBuilder, builder)) {
					match = true;
					break;
				}
			}

			if (!match) {
				typeDetailsBuilder.addMethod(methodBuilder);
			}
		}

		/*
		 * Setter methods for proxy
		 * 
		 * The methods in the proxy will be sorted alphabetically, which makes sense
		 * for the Java type. However, we want to process them in the order the
		 * fields are declared, such that the first database field is the first
		 * field we add to the dataDictionary. This affects the order of the
		 * properties in the desktop client, as well as the primary/secondary
		 * properties in the mobile client.
		 */
		for (JavaSymbolName propertyName : orderedProxyFields.keySet()) {
			JavaType methodReturnType = JavaType.VOID_PRIMITIVE;
			JavaSymbolName methodName = new JavaSymbolName("set" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
			List<JavaType> methodParameterTypes = Collections.singletonList(orderedProxyFields.get(propertyName).getPropertyType());
			List<JavaSymbolName> methodParameterNames = Collections.singletonList(propertyName);
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder());

			// Only add a method if it isn't already present, this leaves the user defined methods in play
			boolean match = false;
			for (MethodMetadataBuilder builder : typeDetailsBuilder.getDeclaredMethods()) {
				if (GwtUtils.methodBuildersEqual(methodBuilder, builder)) {
					match = true;
					break;
				}
			}

			if (!match) {
				typeDetailsBuilder.addMethod(methodBuilder);
			}
		}
		return typeDetailsBuilder.build();
	}

	public List<ClassOrInterfaceTypeDetails> buildType(GwtType destType, ClassOrInterfaceTypeDetails templateClass, List<MemberHoldingTypeDetails> extendsTypes) {
		try {
			if (destType == GwtType.PROXY) {
				return Arrays.asList(buildProxy());
			} else if (destType == GwtType.REQUEST) {
				return Arrays.asList(buildRequest());
			}
			return GwtUtils.buildType(destType, templateClass, extendsTypes);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public String buildUiXml(String templateContents, String destFile) {
		try {
			Transformer transformer = XmlUtils.createIndentingTransformer();
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (systemId.equals("http://dl.google.com/gwt/DTD/xhtml.ent")) {
						return new InputSource(TemplateUtils.getTemplate(GwtMetadata.class, "templates/xhtml.ent"));
					} else {
						// Use the default behaviour
						return null;
					}
				}
			});

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(templateContents));

			Document templateDocument = builder.parse(is);

			is = new InputSource();
			is.setCharacterStream(new FileReader(destFile));
			Document existingDocument = builder.parse(is);

			Element existingHoldingElement = XmlUtils.findFirstElement("//*[@id='" + "boundElementHolder" + "']", existingDocument.getDocumentElement());
			Element templateHoldingElement = XmlUtils.findFirstElement("//*[@id='" + "boundElementHolder" + "']", templateDocument.getDocumentElement());

			if (existingHoldingElement != null) {
				HashMap<String, Element> templateElementMap = new LinkedHashMap<String, Element>();
				for (Element element : XmlUtils.findElements("//*[@id]", templateHoldingElement)) {
					templateElementMap.put(element.getAttribute("id"), element);
				}

				HashMap<String, Element> existingElementMap = new LinkedHashMap<String, Element>();
				for (Element element : XmlUtils.findElements("//*[@id]", existingHoldingElement)) {
					existingElementMap.put(element.getAttribute("id"), element);
				}

				ArrayList<Element> elementsToAdd = new ArrayList<Element>();
				for (String fieldName : templateElementMap.keySet()) {
					if (!existingElementMap.keySet().contains(fieldName)) {
						elementsToAdd.add(templateElementMap.get(fieldName));
					}
				}

				ArrayList<Element> elementsToRemove = new ArrayList<Element>();
				for (String fieldName : existingElementMap.keySet()) {
					if (!templateElementMap.keySet().contains(fieldName)) {
						elementsToRemove.add(existingElementMap.get(fieldName));
					}
				}

				for (Element element : elementsToAdd) {
					Node importedNode = existingDocument.importNode(element, true);
					existingHoldingElement.appendChild(importedNode);
				}

				for (Element element : elementsToRemove) {
					existingHoldingElement.removeChild(element);
				}

				if (elementsToAdd.size() > 0) {
					List<Element> sortedElements = new ArrayList<Element>();
					for (JavaSymbolName fieldName : orderedProxyFields.keySet()) {
						Element element = XmlUtils.findFirstElement("//*[@id='" + fieldName.getSymbolName() + "']", existingHoldingElement);
						if (element != null) {
							sortedElements.add(element);
						}
					}
					for (Element el : sortedElements) {
						existingHoldingElement.removeChild(el);
					}

					for (Element el : sortedElements) {
						existingHoldingElement.appendChild(el);
					}
				}

				if (elementsToAdd.size() > 0 || elementsToRemove.size() > 0) {
					StreamResult result = new StreamResult(new StringWriter());
					DOMSource source = new DOMSource(existingDocument);
					transformer.transform(source, result);

					return result.getWriter().toString();
				}
			}

			return templateContents;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private ClassOrInterfaceTypeDetails buildRequest() {
		String destinationMetadataId = getDestinationMetadataId(GwtType.REQUEST);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();
		// @Service(Employee.class)
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ServiceName")));

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

		return typeDetailsBuilder.build();
	}

	private void buildInstanceRequestMethod(String destinationMetadataId, List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData) {
		// com.google.gwt.requestfactory.shared.InstanceRequest remove()
		List<JavaType> methodReturnTypeArgs = Arrays.asList(mirrorTypeMap.get(GwtType.PROXY), JavaType.VOID_OBJECT);
		JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.InstanceRequest", 0, DataType.TYPE, null, methodReturnTypeArgs);

		buildRequestMethod(destinationMetadataId, methods, methodMetaData, methodReturnType);
	}

	private void buildStaticRequestMethod(String destinationMetadataId, List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData) {
		// com.google.gwt.requestfactory.shared.Request<List<EmployeeProxy>> findAllEmployees();
		List<JavaType> methodReturnTypeArgs = Collections.singletonList(gwtTypeMap.get(methodMetaData.getReturnType()));
		JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.Request", 0, DataType.TYPE, null, methodReturnTypeArgs);

		buildRequestMethod(destinationMetadataId, methods, methodMetaData, methodReturnType);
	}

	private void buildRequestMethod(String destinationMetadataId, List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData, JavaType methodReturnType) {
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
	 * @param gwtType the mirror class we're producing (required)
	 * @return the MID to the mirror class applicable for the current governor (never null)
	 */
	private String getDestinationMetadataId(GwtType gwtType) {
		return PhysicalTypeIdentifier.createIdentifier(mirrorTypeMap.get(gwtType), mirrorTypePath);
	}

	/**
	 * @param gwtType the mirror class we're producing (required)
	 * @return the Java type the mirror class applicable for the current governor (never null)
	 */
	@SuppressWarnings("unused")
	private JavaType getDestinationJavaType(GwtType gwtType) {
		return PhysicalTypeIdentifier.getJavaType(getDestinationMetadataId(gwtType));
	}

	private AnnotationMetadataBuilder createAdditionalAnnotation(JavaType serverType) {
		List<AnnotationAttributeValue<?>> serverTypeAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		serverTypeAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName().getFullyQualifiedTypeName()));
		return new AnnotationMetadataBuilder(serverType, serverTypeAttributes);
	}

	/**
	 * @return a newly-created type annotations list, complete with the @RooGwtMirroredFrom annotation properly setup
	 */
	private List<AnnotationMetadataBuilder> createAnnotations() {
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> rooGwtMirroredFromConfig = new ArrayList<AnnotationAttributeValue<?>>();
		rooGwtMirroredFromConfig.add(new StringAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName().getFullyQualifiedTypeName()));
		annotations.add(new AnnotationMetadataBuilder(new JavaType(RooGwtMirroredFrom.class.getName()), rooGwtMirroredFromConfig));
		return annotations;
	}

	public static String getMetadataIdentifierType() {
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
}
