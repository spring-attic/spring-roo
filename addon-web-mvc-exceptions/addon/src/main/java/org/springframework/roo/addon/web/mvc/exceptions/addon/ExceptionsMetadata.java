package org.springframework.roo.addon.web.mvc.exceptions.addon;

import org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandlers;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for {@link RooExceptionHandlers}.
 *
 * @author Fran Cardoso
 * @since 2.0
 */
public class ExceptionsMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ExceptionsMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType LOGGER_JAVA_TYPE = new JavaType("org.slf4j.Logger");
  private static final JavaType LOGGER_FACTORY_JAVA_TYPE = new JavaType("org.slf4j.LoggerFactory");

  private static final JavaType HTTP_SERVLET_REQUEST = new JavaType(
      "javax.servlet.http.HttpServletRequest");

  private static final String HTTP_SERVLET_REQUEST_PARAM_NAME = "req";
  private static final String EXCEPTION_PARAM_NAME = "e";
  private static final String LOCALE_PARAM_NAME = "locale";

  private ConstructorMetadata constructor;
  private FieldMetadata loggerField;
  private FieldMetadata messageSourceField;
  private MethodMetadata handlerExceptionMethod;
  private MethodMetadata viewHandlerExceptionMethod;

  protected ExceptionsMetadata(String identifier,
      List<ExceptionHandlerAnnotationValues> exceptionHandlers, JavaType aspectName,
      PhysicalTypeMetadata governorPhysicalTypeMetadata, List<FieldMetadata> fieldsMetadata,
      Boolean controller) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    // Static fields
    this.loggerField = getLoggerField();
    ensureGovernorHasField(new FieldMetadataBuilder(loggerField));

    // Check if exists other messageSource
    for (FieldMetadata field : fieldsMetadata) {
      if (field.getFieldType().equals(SpringJavaType.MESSAGE_SOURCE)) {
        this.messageSourceField = field;
        break;
      }
    }

    if (this.messageSourceField == null) {
      ensureGovernorHasField(new FieldMetadataBuilder(getMessageSourceField(controller)));
    }

    // Constructor
    if (!controller) {
      this.constructor = getConstructor();
      ensureGovernorHasConstructor(new ConstructorMetadataBuilder(constructor));
    }

    for (ExceptionHandlerAnnotationValues handler : exceptionHandlers) {
      final ClassOrInterfaceTypeDetails exceptionDetails = handler.getException();
      final JavaType exception = exceptionDetails.getType();
      final String errorView = handler.getErrorView();
      if (errorView != null) {
        // ModelAndView method
        this.viewHandlerExceptionMethod = getViewHandlerExceptionMethod(exception, errorView);
        ensureGovernorHasMethod(new MethodMetadataBuilder(viewHandlerExceptionMethod));
      } else {
        // ResponseEntity method
        this.handlerExceptionMethod = getHandlerExceptionMethod(exception, exceptionDetails);
        ensureGovernorHasMethod(new MethodMetadataBuilder(handlerExceptionMethod));
      }
    }
    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private MethodMetadata getHandlerExceptionMethod(JavaType exception,
      ClassOrInterfaceTypeDetails exceptionDetails) {

    // Define annotations
    List<AnnotationMetadataBuilder> annotationTypes = new ArrayList<AnnotationMetadataBuilder>();
    AnnotationMetadataBuilder responsebody =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    AnnotationMetadataBuilder exceptionHandler =
        new AnnotationMetadataBuilder(SpringJavaType.EXCEPTION_HANDLER);
    exceptionHandler.addClassAttribute("value", exception);
    annotationTypes.add(responsebody);
    annotationTypes.add(exceptionHandler);

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(HTTP_SERVLET_REQUEST));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JdkJavaType.EXCEPTION));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JdkJavaType.LOCALE));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    parameterNames.add(new JavaSymbolName(HTTP_SERVLET_REQUEST_PARAM_NAME));
    parameterNames.add(new JavaSymbolName(EXCEPTION_PARAM_NAME));
    parameterNames.add(new JavaSymbolName(LOCALE_PARAM_NAME));

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("handle".concat(exception.getSimpleTypeName()));
    MethodMetadata existingMethod = getGovernorMethod(methodName);
    if (existingMethod != null) {
      return existingMethod;
    }

    // Set throws types
    List<JavaType> throwTypes = new ArrayList<JavaType>();
    throwTypes.add(JdkJavaType.EXCEPTION);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // String errorMessage =
    bodyBuilder.appendFormalLine(String.format("%s errorMessage = ",
        getNameOfJavaType(JavaType.STRING)));

    // this.messageSource.getMessage("label_my_exception", null, locale);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("this.%s.getMessage(\"label_%s\", null, locale);",
        this.messageSourceField.getFieldName(), exception.getSimpleTypeName().toLowerCase()));
    bodyBuilder.indentRemove();

    // LOG.error(errorMessage, e);
    bodyBuilder.appendFormalLine("LOG.error(errorMessage, e);");

    // return ResponseEntity.status(HttpStatus.ERROR_CODE)
    // .body(Collections.singletonMap("message", errorMessage));
    bodyBuilder.appendFormalLine(String.format("return %s.status(%s)",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        exceptionDetails.getAnnotation(SpringJavaType.RESPONSE_STATUS).getAttribute("value")
            .getValue()));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format(
        ".body(%s.singletonMap(\"message\", errorMessage));", getNameOfJavaType(new JavaType(
            Collections.class))));
    bodyBuilder.indentRemove();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setThrowsTypes(throwTypes);
    methodBuilder.setAnnotations(annotationTypes);
    return methodBuilder.build();

  }

  private MethodMetadata getViewHandlerExceptionMethod(JavaType exception, String errorView) {
    // Define annotations
    List<AnnotationMetadataBuilder> annotationTypes = new ArrayList<AnnotationMetadataBuilder>();
    AnnotationMetadataBuilder exceptionHandler =
        new AnnotationMetadataBuilder(SpringJavaType.EXCEPTION_HANDLER);
    exceptionHandler.addClassAttribute("value", exception);
    annotationTypes.add(exceptionHandler);

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(HTTP_SERVLET_REQUEST));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JdkJavaType.EXCEPTION));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JdkJavaType.LOCALE));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    parameterNames.add(new JavaSymbolName(HTTP_SERVLET_REQUEST_PARAM_NAME));
    parameterNames.add(new JavaSymbolName(EXCEPTION_PARAM_NAME));
    parameterNames.add(new JavaSymbolName(LOCALE_PARAM_NAME));

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("handle".concat(exception.getSimpleTypeName()));
    MethodMetadata existingMethod = getGovernorMethod(methodName);
    if (existingMethod != null) {
      return existingMethod;
    }

    // Set throws types
    List<JavaType> throwTypes = new ArrayList<JavaType>();
    throwTypes.add(JdkJavaType.EXCEPTION);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
    bodyBuilder.appendFormalLine(String.format(
        "if (%s.findAnnotation(e.getClass(), %s.class) != null) {",
        getNameOfJavaType(SpringJavaType.ANNOTATION_UTILS),
        getNameOfJavaType(SpringJavaType.RESPONSE_STATUS)));

    bodyBuilder.indent();

    // throw e;
    bodyBuilder.appendFormalLine("throw e;");

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // String errorMessage =
    bodyBuilder.appendFormalLine(String.format("%s errorMessage = ",
        getNameOfJavaType(JavaType.STRING)));

    // this.messageSource.getMessage("label_my_exception", null, locale);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("this.%s.getMessage(\"label_%s\", null, locale);",
        this.messageSourceField.getFieldName(), exception.getSimpleTypeName().toLowerCase()));
    bodyBuilder.indentRemove();

    // LOG.error(errorMessage, e);
    bodyBuilder.appendFormalLine("LOG.error(errorMessage, e);");

    // ModelAndView mav = new ModelAndView();
    bodyBuilder.appendFormalLine(String.format("%s mav = new %s();",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW),
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW)));

    // mav.addObject("exception", e);
    bodyBuilder.appendFormalLine("mav.addObject(\"exception\", e);");

    // mav.addObject("message", errorMessage);
    bodyBuilder.appendFormalLine("mav.addObject(\"message\", errorMessage);");

    // mav.addObject("url", req.getRequestURL());
    bodyBuilder.appendFormalLine("mav.addObject(\"url\", req.getRequestURL());");

    // mav.setViewName("errorView");
    bodyBuilder.appendFormalLine(String.format("mav.setViewName(\"%s\");", errorView));

    // return mav;
    bodyBuilder.appendFormalLine("return mav;");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setThrowsTypes(throwTypes);
    methodBuilder.setAnnotations(annotationTypes);
    return methodBuilder.build();
  }

  public FieldMetadata getLoggerField() {
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    bodyBuilder.append(getNameOfJavaType(LOGGER_FACTORY_JAVA_TYPE));
    bodyBuilder.append(".getLogger(");
    bodyBuilder.append(governorPhysicalTypeMetadata.getType().getSimpleTypeName());
    bodyBuilder.append(".class)");
    final String initializer = bodyBuilder.getOutput();

    FieldMetadataBuilder field =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL,
            new JavaSymbolName("LOG"), LOGGER_JAVA_TYPE, initializer);

    return field.build();
  }

  private FieldMetadata getMessageSourceField(Boolean controller) {

    // Return current MessageSource field if already exists
    if (this.messageSourceField != null) {
      return this.messageSourceField;
    }

    // Preparing @Autowired annotation
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    if (controller) {
      // MessageSource field must be annotated on controllers
      annotations.add(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
    }

    // Generating field
    FieldMetadataBuilder field =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "exceptionMessageSource"), SpringJavaType.MESSAGE_SOURCE);

    this.messageSourceField = field.build();
    return this.messageSourceField;
  }

  private ConstructorMetadata getConstructor() {
    // Getting MessageSource field name
    final String messageSource = String.valueOf(this.messageSourceField.getFieldName());

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.setModifier(Modifier.PUBLIC);
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    // Adding parameters
    constructor.addParameter(messageSource, SpringJavaType.MESSAGE_SOURCE);

    // Generating body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine(String.format("this.%s = %s;", messageSource, messageSource));

    // Adding body
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();
  }

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

}
