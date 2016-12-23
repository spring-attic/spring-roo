package org.springframework.roo.addon.web.mvc.thymeleaf.addon.link.factory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
  protected static final String DATATABLES_FIELD_NAME = "DATATABLES";
  protected static final String DATATABLES_FIELD_VALUE = "datatables";
  protected static final String CREATE_FIELD_NAME = "CREATE";
  protected static final String CREATE_FIELD_VALUE = "create";
  protected static final String CREATE_FORM_FIELD_NAME = "CREATE_FORM";
  protected static final String CREATE_FORM_FIELD_VALUE = "createForm";

  private static final JavaType JR_EXCEPTION = new JavaType(
      "net.sf.jasperreports.engine.JRException");
  private static final JavaType IO_EXCEPTION = new JavaType("java.io.IOException");
  private static final JavaType CLASS_NOT_FOUND_EXCEPTION = new JavaType(
      "java.lang.ClassNotFoundException");

  private final ControllerMetadata controllerMetadata;
  private final ControllerType controllerType;
  private final String controllerName;
  private final String entityName;
  private final JavaType controller;

  private final MethodMetadata toUriForCollectionControllerMethod;
  private final MethodMetadata toUriForItemControllerMethod;
  private final MethodMetadata toUriForDetailControllerMethod;
  private final MethodMetadata toUriForSearchControllerMethod;
  private final MethodMetadata getControllerClassMethod;

  private final AnnotatedJavaType stringArgument = new AnnotatedJavaType(JavaType.STRING);
  private final AnnotatedJavaType objectArrayArgument =
      new AnnotatedJavaType(JavaType.OBJECT_ARRAY);
  private final AnnotatedJavaType mapStringObjectArgument = new AnnotatedJavaType(
      JavaType.wrapperOf(JdkJavaType.MAP, JavaType.STRING, JavaType.OBJECT));

  private final JavaSymbolName METHOD_NAME_ARGUMENT_NAME = new JavaSymbolName("methodName");
  private final JavaSymbolName PARAMETERS_ARGUMENT_NAME = new JavaSymbolName("parameters");
  private final JavaSymbolName PATH_VARIABLES_ARGUMENT_NAME = new JavaSymbolName("pathVariables");

  private final ImportRegistrationResolver importResolver;

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
   * @param collectionController
   * @param formBeansEnumFields
   * @param formBeansDateTimeFields
   * @param detailsCollectionController
   * @param relatedCollectionController
  
   */
  public LinkFactoryMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType controller,
      final ControllerMetadata controllerMetadata) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.controllerMetadata = controllerMetadata;
    this.controllerType = this.controllerMetadata.getType();
    this.controller = controller;
    this.controllerName = this.controller.getSimpleTypeName();
    this.entityName = this.controllerMetadata.getEntity().getSimpleTypeName();
    this.importResolver = builder.getImportRegistrationResolver();

    // Add @Component
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.COMPONENT));

    // Set implements type
    ensureGovernorImplements(JavaType.wrapperOf(SpringletsJavaType.SPRINGLETS_METHOD_LINK_FACTORY,
        this.controller));

    // Build specific members depending on controller type
    switch (this.controllerType) {
      case COLLECTION: {

        // Create fields
        ensureGovernorHasField(returnStaticStringFieldBuilder("LIST", "list"));
        ensureGovernorHasField(returnStaticStringFieldBuilder(DATATABLES_FIELD_NAME,
            DATATABLES_FIELD_VALUE));
        ensureGovernorHasField(returnStaticStringFieldBuilder(CREATE_FIELD_NAME, CREATE_FIELD_VALUE));
        ensureGovernorHasField(returnStaticStringFieldBuilder(CREATE_FORM_FIELD_NAME,
            CREATE_FORM_FIELD_VALUE));
        ensureGovernorHasField(returnStaticStringFieldBuilder("EXPORT_CSV", "exportCsv"));
        ensureGovernorHasField(returnStaticStringFieldBuilder("EXPORT_PDF", "exportPdf"));
        ensureGovernorHasField(returnStaticStringFieldBuilder("EXPORT_XLS", "exportXls"));

        // Create methods
        this.toUriForCollectionControllerMethod =
            addAndGet(getToUriForCollectionControllerMethod());
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        this.getControllerClassMethod = addAndGet(getControllerClassMethod());
        break;
      }
      case ITEM: {

        // Create fields
        ensureGovernorHasField(returnStaticStringFieldBuilder("SHOW", "show"));
        ensureGovernorHasField(returnStaticStringFieldBuilder("EDIT_FORM", "editForm"));
        ensureGovernorHasField(returnStaticStringFieldBuilder("UPDATE", "update"));
        ensureGovernorHasField(returnStaticStringFieldBuilder("DELETE", "delete"));

        // Create methods
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = addAndGet(getToUriForItemControllerMethod());
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        this.getControllerClassMethod = addAndGet(getControllerClassMethod());
        break;
      }
      case SEARCH: {

        // TODO: this will be implemented shortly
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        this.getControllerClassMethod = addAndGet(getControllerClassMethod());
        return;
      }
      case DETAIL: {

        // Create fields
        ensureGovernorHasField(returnStaticStringFieldBuilder(CREATE_FORM_FIELD_NAME,
            CREATE_FORM_FIELD_VALUE));
        ensureGovernorHasField(returnStaticStringFieldBuilder(CREATE_FIELD_NAME, CREATE_FIELD_VALUE));
        ensureGovernorHasField(returnStaticStringFieldBuilder(DATATABLES_FIELD_NAME,
            DATATABLES_FIELD_VALUE));

        // Create methods
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = addAndGet(getToUriForDetailControllerMethod());
        this.getControllerClassMethod = addAndGet(getControllerClassMethod());
        break;
      }
      default:
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        this.getControllerClassMethod = null;
        return;
    }

    // Build the ITD
    itdTypeDetails = builder.build();
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

    bodyBuilder.appendFormalLine("return %s.class;",
        this.controller.getNameIncludingTypeParameters(false, this.importResolver));

    // return CONTROLLER_CLASS;
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.wrapperOf(
            JavaType.CLASS, this.controller), null);

    // Set method body
    methodBuilder.setBodyBuilder(bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * Generates a `toUri` method which generates URI's for the *Collection* 
   * controller methods which are called from views.
   *
   * @param finderName
   * @param serviceFinderMethod
   * @return
   */
  private MethodMetadata getToUriForCollectionControllerMethod() {

    // Define methodName
    final JavaSymbolName methodName = TO_URI_METHOD_NAME;

    // Define method argument types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(stringArgument);
    parameterTypes.add(objectArrayArgument);
    parameterTypes.add(mapStringObjectArgument);

    // Return method if already exists
    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
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

    // if (METHOD_NAME_ARGUMENT_NAME.equals(LIST)) {
    bodyBuilder.appendFormalLine("if (%s.equals(LIST)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).list(null)).build();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).list(null)).build();",
        SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
            this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(DATATABLES)) {
    bodyBuilder.appendFormalLine("if (%s.equals(%s)) {", METHOD_NAME_ARGUMENT_NAME,
        DATATABLES_FIELD_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).datatables(null, null, null)).replaceQuery(null).build();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return %1$s.fromMethodCall(%1$s.on(getControllerClass())"
        + ".datatables(null, null, null)).replaceQuery(null).build();",
        SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
            this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(CREATE)) {
    bodyBuilder.appendFormalLine("if (%s.equals(%s)) {", METHOD_NAME_ARGUMENT_NAME,
        CREATE_FIELD_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).create(null, null, null)).build();
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(
            "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).create(null, null, null)).build();",
            SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(CREATE_FORM)) {
    bodyBuilder.appendFormalLine("if (%s.equals(%s)) {", METHOD_NAME_ARGUMENT_NAME,
        CREATE_FORM_FIELD_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).createForm(null)).build();
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).createForm(null)).build();",
        SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
            this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(EXPORT_CSV)) {
    bodyBuilder.appendFormalLine("if (%s.equals(EXPORT_CSV)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).exportCsv(null, null, null, null, null)).build();
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(
            "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).exportCsv(null, null, null, null, null)).build();",
            SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(EXPORT_PDF)) {
    bodyBuilder.appendFormalLine("if (%s.equals(EXPORT_PDF)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).exportPdf(null, null, null, null, null)).build();
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(
            "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).exportPdf(null, null, null, null, null)).build();",
            SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(EXPORT_XLS)) {
    bodyBuilder.appendFormalLine("if (%s.equals(EXPORT_XLS)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).exportXls(null, null, null, null)).build();
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(
            "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).exportXls(null, null, null, null, null)).build();",
            SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                this.importResolver));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.newLine();

    // throw new IllegalArgumentException("Invalid method name: " + METHOD_NAME_ARGUMENT_NAME);
    bodyBuilder.appendFormalLine(
        "throw new IllegalArgumentException(\"Invalid method name: \" + %s);",
        METHOD_NAME_ARGUMENT_NAME);

    // Build method builder
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.URI_COMPONENTS, parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  private MethodMetadata getToUriForDetailControllerMethod() {

    // Define methodName
    final JavaSymbolName methodName = TO_URI_METHOD_NAME;

    // Define method argument types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(stringArgument);
    parameterTypes.add(objectArrayArgument);
    parameterTypes.add(mapStringObjectArgument);

    // Return method if already exists
    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
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

    // Assert.notEmpty(PATH_VARIABLES_ARGUMENT_NAME, "CONTROLLER_NAME links need at least "
    bodyBuilder.appendFormalLine(String.format("%s.notEmpty(%s, \"%s links need at least \"",
        SpringJavaType.ASSERT.getNameIncludingTypeParameters(false, this.importResolver),
        PATH_VARIABLES_ARGUMENT_NAME, controllerName));

    // + "the ENTITY_NAME id Path Variable with the 'ENTITY_NAME_UNCAPITALIZED' key");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("+ \"the %s id Path Variable with the '%s' key\");",
        this.entityName, StringUtils.uncapitalize(this.entityName)));
    bodyBuilder.newLine();

    // Assert.notNull(PATH_VARIABLES_ARGUMENT_NAME.get("ENTITY_NAME_UNCAPITALIZED"),
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine(String.format("%s.notNull(%s.get(\"%s\"),",
        SpringJavaType.ASSERT.getNameIncludingTypeParameters(false, this.importResolver),
        PATH_VARIABLES_ARGUMENT_NAME, StringUtils.uncapitalize(this.entityName)));

    // "CONTROLLER_NAME links need at least "
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("\"%s links need at least \"", this.controllerName));

    // + "the ENTITY_NAME id Path Variable with the 'ENTITY_NAME_UNCAPITALIZED' key");
    bodyBuilder.appendFormalLine(String.format("+ \"the %s id Path Variable with the '%s' key\");",
        this.entityName, StringUtils.uncapitalize(this.entityName)));
    bodyBuilder.newLine();
    bodyBuilder.indentRemove();

    // if (METHOD_NAME_ARGUMENT_NAME.equals(CREATE_FORM)) {
    bodyBuilder.appendFormalLine("if (%s.equals(CREATE_FORM)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).createForm(null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(String
            .format(
                "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).createForm(null, null)).buildAndExpand(%2$s);",
                SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                    this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(CREATE)) {
    bodyBuilder.appendFormalLine("if (%s.equals(CREATE)) {", METHOD_NAME_ARGUMENT_NAME);

    // Different implementation for detail composition and aggregation
    bodyBuilder.indent();
    if (controllerMetadata.getLastDetailsInfo().type == JpaRelationType.AGGREGATION) {
      // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).create(null, null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
      bodyBuilder
          .appendFormalLine(String
              .format(
                  "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).create(null, null, null)).buildAndExpand(%2$s);",
                  SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                      this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    } else {
      // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).create(null, null, null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
      bodyBuilder
          .appendFormalLine(String
              .format(
                  "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).create(null, null, null, null)).buildAndExpand(%2$s);",
                  SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                      this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    }
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(DATATABLES)) {
    bodyBuilder.appendFormalLine("if (%s.equals(DATATABLES)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).datatables(null, null, null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format(
        "return %1$s.fromMethodCall(%1$s.on(getControllerClass())"
            + ".datatables(null, null, null, null)).buildAndExpand(%2$s);",
        SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
            this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.newLine();

    // throw new IllegalArgumentException("Invalid method name: " + METHOD_NAME_ARGUMENT_NAME);
    bodyBuilder.appendFormalLine(String.format(
        "throw new IllegalArgumentException(\"Invalid method name: \" + %s);",
        METHOD_NAME_ARGUMENT_NAME));

    // }
    bodyBuilder.reset();

    // Build method builder
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.URI_COMPONENTS, parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  private MethodMetadata getToUriForItemControllerMethod() {

    // Define methodName
    final JavaSymbolName methodName = TO_URI_METHOD_NAME;

    // Define method argument types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(stringArgument);
    parameterTypes.add(objectArrayArgument);
    parameterTypes.add(mapStringObjectArgument);

    // Return method if already exists
    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
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

    // Assert.notEmpty(PATH_VARIABLES_ARGUMENT_NAME, "CONTROLLER_NAME links need at least "
    bodyBuilder.appendFormalLine(String.format("%s.notEmpty(%s, \"%s links need at least \"",
        SpringJavaType.ASSERT.getNameIncludingTypeParameters(false, this.importResolver),
        PATH_VARIABLES_ARGUMENT_NAME, controllerName));

    // + "the ENTITY_NAME id Path Variable with the 'ENTITY_NAME_UNCAPITALIZED' key");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("+ \"the %s id Path Variable with the '%s' key\");",
        this.entityName, StringUtils.uncapitalize(this.entityName)));
    bodyBuilder.newLine();

    // Assert.notNull(PATH_VARIABLES_ARGUMENT_NAME.get("ENTITY_NAME_UNCAPITALIZED"),
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine(String.format("%s.notNull(%s.get(\"%s\"),",
        SpringJavaType.ASSERT.getNameIncludingTypeParameters(false, this.importResolver),
        PATH_VARIABLES_ARGUMENT_NAME, StringUtils.uncapitalize(this.entityName)));

    // "CONTROLLER_NAME links need at least "
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("\"%s links need at least \"", this.controllerName));

    // + "the ENTITY_NAME id Path Variable with the 'ENTITY_NAME_UNCAPITALIZED' key");
    bodyBuilder.appendFormalLine(String.format("+ \"the %s id Path Variable with the '%s' key\");",
        this.entityName, StringUtils.uncapitalize(this.entityName)));
    bodyBuilder.newLine();
    bodyBuilder.indentRemove();

    // if (METHOD_NAME_ARGUMENT_NAME.equals(SHOW)) {
    bodyBuilder.appendFormalLine("if (%s.equals(SHOW)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).show(null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(String
            .format(
                "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).show(null, null)).buildAndExpand(%2$s);",
                SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                    this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(UPDATE)) {
    bodyBuilder.appendFormalLine("if (%s.equals(UPDATE)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).update(null, null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(String
            .format(
                "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).update(null, null, null)).buildAndExpand(%2$s);",
                SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                    this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(EDIT_FORM)) {
    bodyBuilder.appendFormalLine("if (%s.equals(EDIT_FORM)) {", METHOD_NAME_ARGUMENT_NAME);

    // return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).editForm(null, null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(String
            .format(
                "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).editForm(null, null)).buildAndExpand(%2$s);",
                SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                    this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // if (METHOD_NAME_ARGUMENT_NAME.equals(DELETE)) {
    bodyBuilder.appendFormalLine("if (%s.equals(DELETE)) {", METHOD_NAME_ARGUMENT_NAME);

    // MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(getControllerClass()).delete(null)).buildAndExpand(PATH_VARIABLES_ARGUMENT_NAME);
    bodyBuilder.indent();
    bodyBuilder
        .appendFormalLine(String
            .format(
                "return %1$s.fromMethodCall(%1$s.on(getControllerClass()).delete(null)).buildAndExpand(%2$s);",
                SpringJavaType.MVC_URI_COMPONENTS_BUILDER.getNameIncludingTypeParameters(false,
                    this.importResolver), PATH_VARIABLES_ARGUMENT_NAME));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.newLine();

    // throw new IllegalArgumentException("Invalid method name: " + METHOD_NAME_ARGUMENT_NAME);
    bodyBuilder.appendFormalLine(String.format(
        "throw new IllegalArgumentException(\"Invalid method name: \" + %s);",
        METHOD_NAME_ARGUMENT_NAME));

    bodyBuilder.reset();

    // Build method builder
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.URI_COMPONENTS, parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  private MethodMetadata getToUriForSearchControllerMethod() {
    // TODO Auto-generated method stub
    return null;
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
   * @param fieldName
   * @param initializer
   * @return FieldMetadataBuilder
   */
  private FieldMetadataBuilder returnStaticStringFieldBuilder(String fieldName, String initializer) {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE + Modifier.STATIC + Modifier.FINAL,
        new JavaSymbolName(fieldName), JavaType.STRING, "\"" + initializer + "\"");
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
