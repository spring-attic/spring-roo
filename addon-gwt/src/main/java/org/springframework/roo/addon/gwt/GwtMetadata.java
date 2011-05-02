package org.springframework.roo.addon.gwt;

import java.io.File;
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
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
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
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	private ClassOrInterfaceTypeDetails governorTypeDetails;
	private ProjectMetadata projectMetadata;
	private List<MethodMetadata> proxyMethods;
	private List<MethodMetadata> requestMethods;
	private MethodMetadata findAllMethod;
	private MethodMetadata findMethod;
	private MethodMetadata countMethod;
	private MethodMetadata findEntriesMethod;

	public GwtMetadata(String identifier, ClassOrInterfaceTypeDetails governorTypeDetails, ProjectMetadata projectMetadata, List<MethodMetadata> proxyMethods, List<MethodMetadata> requestMethods, MethodMetadata findAllMethod, MethodMetadata findMethod, MethodMetadata findEntriesMethod, MethodMetadata countMethod) {
		super(identifier);
		Assert.notNull(governorTypeDetails, "Governor details are required");
		Assert.notNull(projectMetadata, "ProjectMetadata is required");
		Assert.notNull(proxyMethods, "Proxy methods are required");
		Assert.notNull(requestMethods, "Request methods are required");
		Assert.notNull(findAllMethod, "findAllMethod required");
		Assert.notNull(findMethod, "findMethod required");
		Assert.notNull(findEntriesMethod, "findEntriesMethod required");
		Assert.notNull(countMethod, "countMethod required");
		this.governorTypeDetails = governorTypeDetails;
		this.projectMetadata = projectMetadata;
		this.proxyMethods = proxyMethods;
		this.requestMethods = requestMethods;
		this.findAllMethod = findAllMethod;
		this.findMethod = findMethod;
		this.findEntriesMethod = findEntriesMethod;
		this.countMethod = countMethod;
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

		List<MethodMetadataBuilder> methods = new LinkedList<MethodMetadataBuilder>();
		for (MethodMetadata method : proxyMethods) {
			MethodMetadataBuilder abstractAccessorMethodBuilder = new MethodMetadataBuilder(destinationMetadataId, method);
			abstractAccessorMethodBuilder.setBodyBuilder(new InvocableMemberBodyBuilder());
			abstractAccessorMethodBuilder.setModifier(Modifier.ABSTRACT);
			methods.add(abstractAccessorMethodBuilder);

			String propertyName = method.getMethodName().getSymbolName().replaceFirst("get", "");
			MethodMetadataBuilder abstractMutatorMethodBuilder = new MethodMetadataBuilder(destinationMetadataId, method);
			abstractMutatorMethodBuilder.setBodyBuilder(new InvocableMemberBodyBuilder());
			abstractMutatorMethodBuilder.setModifier(Modifier.ABSTRACT);
			abstractMutatorMethodBuilder.setReturnType(JavaType.VOID_PRIMITIVE);
			abstractMutatorMethodBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(Arrays.asList(method.getReturnType())));
			abstractMutatorMethodBuilder.setParameterNames(Arrays.asList(new JavaSymbolName(StringUtils.uncapitalize(propertyName))));
			abstractMutatorMethodBuilder.setMethodName(new JavaSymbolName(method.getMethodName().getSymbolName().replaceFirst("get", "set")));
			methods.add(abstractMutatorMethodBuilder);
		}

		typeDetailsBuilder.setDeclaredMethods(methods);

		return typeDetailsBuilder.build();
	}

	public ClassOrInterfaceTypeDetails buildRequest() {
		String destinationMetadataId = getDestinationMetadataId(GwtType.REQUEST);
		JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

		List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();
		// @Service(Employee.class)
		typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ServiceName")));

		List<MethodMetadataBuilder> methods = new LinkedList<MethodMetadataBuilder>();
		for (MethodMetadata method : requestMethods) {
			methods.add(getRequestMethod(destinationMetadataId, method));
		}

		List<JavaType> extendsTypes = Collections.singletonList(new JavaType("com.google.gwt.requestfactory.shared.RequestContext"));

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(destinationMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);
		typeDetailsBuilder.setAnnotations(typeAnnotations);
		typeDetailsBuilder.setDeclaredMethods(methods);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);

		return typeDetailsBuilder.build();
	}

	public String buildUiXml(String templateContents, String destFile) {
		try {

			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (systemId.equals("http://dl.google.com/gwt/DTD/xhtml.ent")) {
						return new InputSource(TemplateUtils.getTemplate(GwtMetadata.class, "templates/xhtml.ent"));
					}
					
					// Use the default behaviour
					return null;
				}
			});

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(templateContents));

			Document templateDocument = builder.parse(is);

			if (!new File(destFile).exists()) {
				return transformXml(templateDocument);
			}

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

				if (existingElementMap.keySet().containsAll(templateElementMap.values())) {
					return transformXml(existingDocument);
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
					for (MethodMetadata method : proxyMethods) {
						String propertyName = StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName());
						Element element = XmlUtils.findFirstElement("//*[@id='" + propertyName + "']", existingHoldingElement);
						if (element != null) {
							sortedElements.add(element);
						}
					}
					for (Element el : sortedElements) {
						if (el.getParentNode() != null && el.getParentNode().equals(existingHoldingElement)) {
							existingHoldingElement.removeChild(el);
						}
					}

					for (Element el : sortedElements) {
						existingHoldingElement.appendChild(el);
					}
				}

				return transformXml(existingDocument);
			}

			return transformXml(templateDocument);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String transformXml(Document document) throws TransformerException {
		Transformer transformer = XmlUtils.createIndentingTransformer();
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(document);
		transformer.transform(source, result);
		return result.getWriter().toString();
	}

	private MethodMetadataBuilder getRequestMethod(String destinationMetadataId, MethodMetadata methodMetaData) {
		if (methodMetaData.getMethodName().equals(countMethod.getMethodName()) || methodMetaData.getMethodName().equals(findMethod.getMethodName()) || methodMetaData.getMethodName().equals(findAllMethod.getMethodName()) || methodMetaData.getMethodName().equals(findEntriesMethod.getMethodName())) {
			List<JavaType> methodReturnTypeArgs = Collections.singletonList(methodMetaData.getReturnType());
			JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.Request", 0, DataType.TYPE, null, methodReturnTypeArgs);
			return getRequestMethod(destinationMetadataId, methodMetaData, methodReturnType);
		}
		
		JavaType proxy = PhysicalTypeIdentifier.getJavaType(getDestinationMetadataId(GwtType.PROXY));
		List<JavaType> methodReturnTypeArgs = Arrays.asList(proxy, JavaType.VOID_OBJECT);
		JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.InstanceRequest", 0, DataType.TYPE, null, methodReturnTypeArgs);
		return getRequestMethod(destinationMetadataId, methodMetaData, methodReturnType);
	}

	private MethodMetadataBuilder getRequestMethod(String destinationMetadataId, MethodMetadata methodMetaData, JavaType methodReturnType) {
		return new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodMetaData.getMethodName(), methodReturnType, methodMetaData.getParameterTypes(), methodMetaData.getParameterNames(), new InvocableMemberBodyBuilder());
	}

	/**
	 * @param gwtType the mirror class we're producing (required)
	 * @return the MID to the mirror class applicable for the current governor (never null)
	 */
	private String getDestinationMetadataId(GwtType gwtType) {
		return PhysicalTypeIdentifier.createIdentifier(GwtUtils.convertGovernorTypeNameIntoKeyTypeName(governorTypeDetails.getName(), gwtType, projectMetadata), Path.SRC_MAIN_JAVA);
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
