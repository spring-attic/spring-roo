package org.springframework.roo.addon.web.mvc.thymeleaf.addon.link.factory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * = LinkFactoryMetadata
 * 
 * Metadata for {@link RooThymeleaf}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
public class LinkFactoryMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected static final JavaSymbolName TO_URI_METHOD_NAME = new JavaSymbolName("toUri");
  protected static final JavaSymbolName GET_CONTROLLER_CLASS_METHOD_NAME = new JavaSymbolName(
      "getControllerClass");

  private final JavaType controller;
  private final List<MethodMetadata> controllerMethods;

  private final MethodMetadata toUriMethod;
  private final MethodMetadata getControllerClassMethod;

  private Map<String, FieldMetadataBuilder> constantForMethods;


  private final AnnotatedJavaType stringArgument = new AnnotatedJavaType(JavaType.STRING);
  private final AnnotatedJavaType objectArrayArgument =
      new AnnotatedJavaType(JavaType.OBJECT_ARRAY);
  private final AnnotatedJavaType mapStringObjectArgument = new AnnotatedJavaType(
      JavaType.wrapperOf(JdkJavaType.MAP, JavaType.STRING, JavaType.OBJECT));

  private final JavaSymbolName METHOD_NAME_ARGUMENT_NAME = new JavaSymbolName("methodName");
  private final JavaSymbolName PARAMETERS_ARGUMENT_NAME = new JavaSymbolName("parameters");
  private final JavaSymbolName PATH_VARIABLES_ARGUMENT_NAME = new JavaSymbolName("pathVariables");

  private static final String PROVIDES_TYPE_STRING = LinkFactoryMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

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
   * @param controller
   * @param controllerMetadata
   */
  public LinkFactoryMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType controller,
      final ControllerMetadata controllerMetadata, final List<MethodMetadata> controllerMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.controller = controller;
    this.controllerMethods = controllerMethods;
    this.constantForMethods = new HashMap<String, FieldMetadataBuilder>();


    // Add @Component
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.COMPONENT));

    // Set implements type
    ensureGovernorImplements(JavaType.wrapperOf(SpringletsJavaType.SPRINGLETS_METHOD_LINK_FACTORY,
        this.controller));

    // Including constants for every method existing in the controller class
    for (MethodMetadata method : controllerMethods) {
      String methodName = method.getMethodName().getSymbolName();
      if (constantForMethods.get(methodName) == null) {
        ensureGovernorHasField(getConstantForMethodName(methodName));
      }
    }

    // Create methods
    this.getControllerClassMethod = addAndGet(getControllerClassMethod());
    this.toUriMethod = addAndGet(getToUriMethod());

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
     * Generates a `toUri` method which generates URI's for the *Collection* 
     * controller methods which are called from views.
     *
     * @param finderName
     * @param serviceFinderMethod
     * @return
     */
  private MethodMetadata getToUriMethod() {

    // Define methodName
    final JavaSymbolName toUriMethodName = TO_URI_METHOD_NAME;

    // Define method argument types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(stringArgument);
    parameterTypes.add(objectArrayArgument);
    parameterTypes.add(mapStringObjectArgument);

    // Return method if already exists
    MethodMetadata existingMethod =
        getGovernorMethod(toUriMethodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Define method argument names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(METHOD_NAME_ARGUMENT_NAME);
    parameterNames.add(PARAMETERS_ARGUMENT_NAME);
    parameterNames.add(PATH_VARIABLES_ARGUMENT_NAME);

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Include a conditional for every method
    for (MethodMetadata method : this.controllerMethods) {
      // Getting methodName
      String methodName = method.getMethodName().getSymbolName();

      // Getting methodParams
      String methodParamsToNull = "";
      for (int i = 0; i < method.getParameterTypes().size(); i++) {
        // Include a null declaration for every parameter
        methodParamsToNull += "null, ";
      }
      // Remove empty space and comma
      if (StringUtils.isNotEmpty(methodParamsToNull)) {
        methodParamsToNull = methodParamsToNull.substring(0, methodParamsToNull.length() - 2);
      }

      // if (METHOD_NAME_ARGUMENT_NAME.equals(methodNameConstant)) {
      bodyBuilder.appendFormalLine("if (%s.equals(%s)) {", METHOD_NAME_ARGUMENT_NAME,
          getConstantForMethodName(methodName).getFieldName());
      bodyBuilder.indent();

      // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).list(null)).build();

      bodyBuilder.appendFormalLine(
          "return %1$s.fromMethodCall(%1$s.on(%2$s()).%3$s(%4$s)).buildAndExpand(%5$s);",
          getNameOfJavaType(SpringletsJavaType.SPRINGLETS_MVC_URI_COMPONENTS_BUILDER),
          this.getControllerClassMethod.getMethodName(), methodName, methodParamsToNull,
          PATH_VARIABLES_ARGUMENT_NAME);
      bodyBuilder.indentRemove();
      // }
      bodyBuilder.appendFormalLine("}");
    }

    // throw new IllegalArgumentException("Invalid method name: " + METHOD_NAME_ARGUMENT_NAME);
    bodyBuilder.appendFormalLine(
        "throw new IllegalArgumentException(\"Invalid method name: \" + %s);",
        METHOD_NAME_ARGUMENT_NAME);

    // Build method builder
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, toUriMethodName,
            SpringJavaType.URI_COMPONENTS, parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  private MethodMetadata getControllerClassMethod() {

    // Define methodName
    final JavaSymbolName methodName = GET_CONTROLLER_CLASS_METHOD_NAME;

    // Define method argument types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Return method if already exists
    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    bodyBuilder.appendFormalLine("return %s.class;", getNameOfJavaType(this.controller));

    // return CONTROLLER_CLASS;
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.wrapperOf(
            JavaType.CLASS, this.controller), null);

    // Set method body
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * Add method to governor if needed and returns the MethodMetadata.
   * 
   * @param method the MethodMetadata to add and return.
   * @return MethodMetadata
   */
  private MethodMetadata addAndGet(MethodMetadata method) {
    ensureGovernorHasMethod(new MethodMetadataBuilder(method));
    return method;
  }

  /**
   * Builds and returns a private static final field with provided field name and initializer
   * 
   * @param methodName
   * @return
   */
  private FieldMetadataBuilder getConstantForMethodName(String methodName) {

    // If already exists, return the existing one
    if (constantForMethods.get(methodName) != null) {
      return constantForMethods.get(methodName);
    }

    // Create a new one and cache it
    FieldMetadataBuilder constant =
        new FieldMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL,
            new JavaSymbolName(methodName.toUpperCase()), JavaType.STRING, "\"" + methodName + "\"");

    constantForMethods.put(methodName, constant);

    return constant;
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

  public List<MethodMetadata> getControllerMethods() {
    return controllerMethods;
  }

}
