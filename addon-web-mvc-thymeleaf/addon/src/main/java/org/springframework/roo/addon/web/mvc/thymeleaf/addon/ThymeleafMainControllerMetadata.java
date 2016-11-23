package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafMainController;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for {@link RooThymeleafMainController}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class ThymeleafMainControllerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ThymeleafMainControllerMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType CONTROLLER_ANNOTATION = new JavaType(
      "org.springframework.stereotype.Controller");

  private final MethodMetadata indexMethod;
  private final MethodMetadata javasrcriptTemplatesMethod;
  private final MethodMetadata accessibilityMethod;

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
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
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   */
  public ThymeleafMainControllerMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.indexMethod = getIndexMethod();

    this.accessibilityMethod = getAccessibilityMethod();

    this.javasrcriptTemplatesMethod = getJavascriptTemplatesmethod();

    // Add @Controller annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONTROLLER_ANNOTATION));

    // Add methods
    ensureGovernorHasMethod(new MethodMetadataBuilder(indexMethod));
    ensureGovernorHasMethod(new MethodMetadataBuilder(accessibilityMethod));
    ensureGovernorHasMethod(new MethodMetadataBuilder(javasrcriptTemplatesMethod));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /*
   * =====================================================================================
   */

  /**
   * @return
   */
  private MethodMetadata getJavascriptTemplatesmethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("javascriptTemplates");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Create @PathVariable("templeate") String template parameter
    AnnotationMetadataBuilder pathVarialbeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVarialbeAnnotation.addStringAttribute("value", "template");
    AnnotatedJavaType templateParameter =
        new AnnotatedJavaType(JavaType.STRING, pathVarialbeAnnotation.build());
    parameterTypes.add(templateParameter);

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("template"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    AnnotationMetadataBuilder requestMapping =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_MAPPING);
    requestMapping.addStringAttribute("value", "/js/{template}.js");
    getNameOfJavaType(SpringJavaType.REQUEST_METHOD);
    requestMapping.addEnumAttribute("method", SpringJavaType.REQUEST_METHOD, "GET");
    annotations.add(requestMapping);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (StringUtils.hasLength(template)) {
    //    return template.concat(".js");
    // }
    bodyBuilder.appendFormalLine("if (%s.hasLength(template)) {",
        getNameOfJavaType(SpringJavaType.STRING_UTILS));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return template.concat(\".js\");");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // throw new NotFoundException("File not found")
    bodyBuilder.appendFormalLine("throw new %s(\"File not found\");",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_NOT_FOUND_EXCEPTION));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "index" method that returns Thymeleaf view
   *
   * @return MethodMetadata
   */
  private MethodMetadata getIndexMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("index");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMapping =
        new AnnotationMetadataBuilder(SpringJavaType.GET_MAPPING);
    getMapping.addStringAttribute("value", "/");
    annotations.add(getMapping);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Always save locale
    bodyBuilder.appendFormalLine(
        "model.addAttribute(\"application_locale\", %s.getLocale().getLanguage());",
        getNameOfJavaType(SpringJavaType.LOCALE_CONTEXT_HOLDER));

    // return "index";
    bodyBuilder.appendFormalLine("return \"index\";");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getAccessibilityMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("accessibility");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMapping =
        new AnnotationMetadataBuilder(SpringJavaType.GET_MAPPING);
    getMapping.addStringAttribute("value", "/accessibility");
    annotations.add(getMapping);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Always save locale
    bodyBuilder.appendFormalLine(
        "model.addAttribute(\"application_locale\", %s.getLocale().getLanguage());",
        getNameOfJavaType(SpringJavaType.LOCALE_CONTEXT_HOLDER));

    // return "accessibility";
    bodyBuilder.appendFormalLine("return \"accessibility\";");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method returns the index method of Thymeleaf
   * main controller
   *
   * @return
   */
  public MethodMetadata getCurrentIndexMethod() {
    return this.indexMethod;
  }


  /**
   * Returns the method which handles javascript templates request
   * @return
   */
  public MethodMetadata getJavasrcriptTemplates() {
    return javasrcriptTemplatesMethod;
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
}
