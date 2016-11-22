package org.springframework.roo.addon.ws.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.ws.annotations.RooSei;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Metadata for {@link RooSei}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class SeiMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(SeiMetadata.class);

  private static final String PROVIDES_TYPE_STRING = SeiMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final JavaPackage projectTopLevelPackage;
  private final ClassOrInterfaceTypeDetails sei;
  private final JavaType service;
  private final List<MethodMetadata> serviceMethods;
  private Map<MethodMetadata, MethodMetadata> seiMethods;
  private Map<MethodMetadata, MethodMetadata> seiMethodsFromServiceMethods;

  private JavaType wsdlDocumentationType = new JavaType(
      "org.apache.cxf.annotations.WSDLDocumentation");

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Constructor
   *
   * @param identifier
   *            the identifier for this item of metadata (required)
   * @param aspectName
   *            the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata
   *            the governor, which is expected to contain a
   *            {@link ClassOrInterfaceTypeDetails} (required)
   * @param projectTopLevelPackage the base package of the project that will be 
   * 								used to create the targetNamespace           
   * @param sei the annotated sei
   * @param service the service related with this SEI
   * @param serviceMethods
   * 			the methods registered in based service
   */
  public SeiMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaPackage projectTopLevelPackage,
      ClassOrInterfaceTypeDetails sei, JavaType service, List<MethodMetadata> serviceMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.projectTopLevelPackage = projectTopLevelPackage;
    this.sei = sei;
    this.service = service;
    this.serviceMethods = serviceMethods;

    // Initialize collections
    seiMethodsFromServiceMethods = new HashMap<MethodMetadata, MethodMetadata>();
    seiMethods = new TreeMap<MethodMetadata, MethodMetadata>();

    // Include @WebService annotation
    AnnotationMetadataBuilder webServiceAnnotation =
        new AnnotationMetadataBuilder(JavaType.WEB_SERVICE);
    webServiceAnnotation.addStringAttribute("name", sei.getType().getSimpleTypeName());
    webServiceAnnotation.addStringAttribute(
        "targetNamespace",
        String.format("http://ws.%s/", StringUtils.reverseDelimited(
            projectTopLevelPackage.getFullyQualifiedPackageName(), '.')));
    ensureGovernorIsAnnotated(webServiceAnnotation);

    // Include @WSDLDocumentation annotation
    AnnotationMetadataBuilder documentationAnnotation =
        new AnnotationMetadataBuilder(wsdlDocumentationType);
    documentationAnnotation.addStringAttribute("value", String.format(
        "TODO Auto-generated documentation for %s", sei.getType().getSimpleTypeName()));
    documentationAnnotation.addEnumAttribute("placement", wsdlDocumentationType,
        new JavaSymbolName("Placement.TOP"));
    ensureGovernorIsAnnotated(documentationAnnotation);

    // Include the same methods as the provided service
    for (MethodMetadata serviceMethod : serviceMethods) {
      ensureGovernorHasMethod(new MethodMetadataBuilder(
          getSEIMethodFromServiceMethod(serviceMethod)));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method obtains a SEI method from a provided service method.
   * 
   * This method caches the generated methods
   * 
   * @param serviceMethod defined in a service interface
   * 
   * @return MethodMetadataBuilder that contains all the information about the new SEI method.
   */
  private MethodMetadata getSEIMethodFromServiceMethod(MethodMetadata serviceMethod) {

    // Check if already exists the method
    if (seiMethodsFromServiceMethods.get(serviceMethod) != null) {
      return seiMethodsFromServiceMethods.get(serviceMethod);
    }

    // If not exists, generate it and cache it.

    // Obtain the necessary elements from service method
    JavaSymbolName methodName = serviceMethod.getMethodName();
    JavaType returnType = serviceMethod.getReturnType();
    List<AnnotatedJavaType> parameterTypes = serviceMethod.getParameterTypes();
    List<JavaSymbolName> parameterNames = serviceMethod.getParameterNames();

    // Obtain parameterList
    // Is necessary to change the method name to prevent errors
    String paramList = "";
    for (AnnotatedJavaType param : parameterTypes) {
      paramList =
          paramList.concat(StringUtils.capitalize(param.getJavaType().getSimpleTypeName())).concat(
              "And");
    }

    if (StringUtils.isNotBlank(paramList)) {

      // Before to update, check if is a finder
      if (methodName.toString().startsWith("findBy")) {
        methodName = new JavaSymbolName("find");
      } else if (methodName.toString().startsWith("countBy")) {
        methodName = new JavaSymbolName("count");
      }

      paramList = paramList.substring(0, paramList.length() - "And".length());
      methodName = new JavaSymbolName(methodName.toString().concat("By").concat(paramList));
    }

    // Annotate parameter types with @WebParam and @XmlJavaTypeAdapter if needed
    List<AnnotatedJavaType> annotatedParameterTypes = new ArrayList<AnnotatedJavaType>();
    for (int i = 0; i < parameterTypes.size(); i++) {
      List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
      // Getting parameter type and parameter name
      AnnotatedJavaType paramType = parameterTypes.get(i);
      JavaSymbolName paramName = parameterNames.get(i);
      // Creating @WebParam annotation
      AnnotationMetadataBuilder webParamAnnotation =
          new AnnotationMetadataBuilder(JavaType.WEB_PARAM);
      webParamAnnotation.addStringAttribute("name", paramName.toString());
      webParamAnnotation.addStringAttribute("targetNamespace", "");
      annotations.add(webParamAnnotation.build());

      // Creating @XmlJavaTypeAdapter annotation
      AnnotationMetadataBuilder javaTypeAdapter =
          new AnnotationMetadataBuilder(JavaType.XML_JAVATYPE_ADAPTER);
      if (paramType.getJavaType().getFullyQualifiedTypeName()
          .equals(JavaType.ITERABLE.getFullyQualifiedTypeName())) {
        javaTypeAdapter.addClassAttribute("value", SpringletsJavaType.SPRINGLETS_ITERABLE_ADAPTER);
        annotations.add(javaTypeAdapter.build());
      } else if (paramType.getJavaType().getFullyQualifiedTypeName()
          .equals(SpringJavaType.PAGE.getFullyQualifiedTypeName())) {
        javaTypeAdapter.addClassAttribute("value", SpringletsJavaType.SPRINGLETS_PAGE_ADAPTER);
        annotations.add(javaTypeAdapter.build());
      } else if (paramType.getJavaType().equals(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH)) {
        javaTypeAdapter.addClassAttribute("value",
            SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH_ADAPTER);
        annotations.add(javaTypeAdapter.build());
      } else if (paramType.getJavaType().equals(SpringJavaType.PAGEABLE)) {
        javaTypeAdapter.addClassAttribute("value", SpringletsJavaType.SPRINGLETS_PAGEABLE_ADAPTER);
        annotations.add(javaTypeAdapter.build());
      }

      // Creating new parameter type annotated with @WebParam
      AnnotatedJavaType annotatedParam =
          new AnnotatedJavaType(paramType.getJavaType(), annotations);
      annotatedParameterTypes.add(annotatedParam);
    }

    MethodMetadataBuilder seiMethod =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, methodName,
            returnType, annotatedParameterTypes, parameterNames, null);

    // Include @XmlJavaTypeAdapter annotation if needed
    AnnotationMetadataBuilder javaTypeAdapter =
        new AnnotationMetadataBuilder(JavaType.XML_JAVATYPE_ADAPTER);
    if (returnType.getFullyQualifiedTypeName()
        .equals(JavaType.ITERABLE.getFullyQualifiedTypeName())) {
      javaTypeAdapter.addClassAttribute("value", SpringletsJavaType.SPRINGLETS_ITERABLE_ADAPTER);
      seiMethod.addAnnotation(javaTypeAdapter);
    } else if (returnType.getFullyQualifiedTypeName().equals(
        SpringJavaType.PAGE.getFullyQualifiedTypeName())) {
      javaTypeAdapter.addClassAttribute("value", SpringletsJavaType.SPRINGLETS_PAGE_ADAPTER);
      seiMethod.addAnnotation(javaTypeAdapter);
    } else if (returnType.equals(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH)) {
      javaTypeAdapter.addClassAttribute("value",
          SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH_ADAPTER);
      seiMethod.addAnnotation(javaTypeAdapter);
    } else if (returnType.equals(SpringJavaType.PAGEABLE)) {
      javaTypeAdapter.addClassAttribute("value", SpringletsJavaType.SPRINGLETS_PAGEABLE_ADAPTER);
      seiMethod.addAnnotation(javaTypeAdapter);
    }

    // Include @RequestWrapper annotation
    AnnotationMetadataBuilder requestWrapperAnnotation =
        new AnnotationMetadataBuilder(JavaType.REQUEST_WRAPPER);
    requestWrapperAnnotation.addStringAttribute("className", String.format("%s.%sRequest", sei
        .getType().getPackage(), seiMethod.getMethodName().getSymbolNameCapitalisedFirstLetter()));
    requestWrapperAnnotation
        .addStringAttribute("localName", String.format("%sRequest", seiMethod.getMethodName()
            .getSymbolNameCapitalisedFirstLetter()));
    requestWrapperAnnotation.addStringAttribute(
        "targetNamespace",
        String.format("http://ws.%s/", StringUtils.reverseDelimited(
            projectTopLevelPackage.getFullyQualifiedPackageName(), '.')));
    seiMethod.addAnnotation(requestWrapperAnnotation);

    // Include @ResponseWrapper annotation
    AnnotationMetadataBuilder responseWrapperAnnotation =
        new AnnotationMetadataBuilder(JavaType.RESPONSE_WRAPPER);
    responseWrapperAnnotation.addStringAttribute("className", String.format("%s.%sResponse", sei
        .getType().getPackage(), seiMethod.getMethodName().getSymbolNameCapitalisedFirstLetter()));
    responseWrapperAnnotation.addStringAttribute("localName", String.format("%sResponse", seiMethod
        .getMethodName().getSymbolNameCapitalisedFirstLetter()));
    responseWrapperAnnotation.addStringAttribute(
        "targetNamespace",
        String.format("http://ws.%s/", StringUtils.reverseDelimited(
            projectTopLevelPackage.getFullyQualifiedPackageName(), '.')));
    seiMethod.addAnnotation(responseWrapperAnnotation);

    // Include @WebMethod annotation
    AnnotationMetadataBuilder webMethodAnnotation =
        new AnnotationMetadataBuilder(JavaType.WEB_METHOD);
    webMethodAnnotation.addStringAttribute("action",
        String.format("urn:%s", seiMethod.getMethodName().getSymbolNameCapitalisedFirstLetter()));
    seiMethod.addAnnotation(webMethodAnnotation);

    // Include @WebResult annotation
    AnnotationMetadataBuilder webResultAnnotation =
        new AnnotationMetadataBuilder(JavaType.WEB_RESULT);
    webResultAnnotation.addStringAttribute("name", returnType.getBaseType().getSimpleTypeName()
        .toLowerCase());
    webResultAnnotation.addStringAttribute("targetNamespace", "");
    seiMethod.addAnnotation(webResultAnnotation);

    // Include @WSDLDocumentationCollection annotation
    AnnotationMetadataBuilder wsdlDocumentationCollectionAnnotation =
        new AnnotationMetadataBuilder(new JavaType(
            "org.apache.cxf.annotations.WSDLDocumentationCollection"));

    // Create @WSDLDocumentation annotation
    List<AnnotationAttributeValue<?>> documentations = new ArrayList<AnnotationAttributeValue<?>>();

    AnnotationMetadataBuilder documentationAnnotation1 =
        new AnnotationMetadataBuilder(wsdlDocumentationType);
    documentationAnnotation1.addStringAttribute("value", String.format(
        "TODO Auto-generated documentation for %s", sei.getType().getSimpleTypeName()));
    documentationAnnotation1.addEnumAttribute("placement", wsdlDocumentationType,
        new JavaSymbolName("Placement.DEFAULT"));
    NestedAnnotationAttributeValue newDocumentation1 =
        new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
            documentationAnnotation1.build());
    documentations.add(newDocumentation1);

    AnnotationMetadataBuilder documentationAnnotation2 =
        new AnnotationMetadataBuilder(wsdlDocumentationType);
    documentationAnnotation2.addStringAttribute("value", String.format(
        "TODO Auto-generated documentation for %s", sei.getType().getSimpleTypeName()));
    documentationAnnotation2.addEnumAttribute("placement", wsdlDocumentationType,
        new JavaSymbolName("Placement.PORT_TYPE_OPERATION_OUTPUT"));
    NestedAnnotationAttributeValue newDocumentation2 =
        new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
            documentationAnnotation2.build());
    documentations.add(newDocumentation2);

    ArrayAttributeValue<AnnotationAttributeValue<?>> newDocumentations =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            documentations);

    wsdlDocumentationCollectionAnnotation.addAttribute(newDocumentations);
    seiMethod.addAnnotation(wsdlDocumentationCollectionAnnotation);

    seiMethodsFromServiceMethods.put(serviceMethod, seiMethod.build());
    seiMethods.put(seiMethod.build(), serviceMethod);

    return seiMethod.build();
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }

  public JavaType getService() {
    return this.service;
  }

  public List<MethodMetadata> getServiceMethods() {
    return this.serviceMethods;
  }

  public Map<MethodMetadata, MethodMetadata> getSeiMethods() {
    return this.seiMethods;
  }
}
