package org.springframework.roo.addon.web.mvc.thymeleaf.addon.link.factory;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for {@link RooThymeleaf}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class LinkFactoryMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected static final JavaSymbolName TO_URI_METHOD_NAME = new JavaSymbolName("toUri");

  private final ControllerMetadata controllerMetadata;
  private final ControllerType controllerType;

  private final MethodMetadata toUriForCollectionControllerMethod;
  private final MethodMetadata toUriForItemControllerMethod;
  private final MethodMetadata toUriForDetailControllerMethod;
  private final MethodMetadata toUriForSearchControllerMethod;

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
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final ControllerMetadata controllerMetadata) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.controllerMetadata = controllerMetadata;
    this.controllerType = this.controllerMetadata.getType();

    switch (this.controllerType) {
      case COLLECTION: {
        this.toUriForCollectionControllerMethod =
            addAndGet(getToUriForCollectionControllerMethod());
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        break;
      }
      case ITEM: {
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = addAndGet(getToUriForItemControllerMethod());
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        break;
      }
      case SEARCH: {
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = addAndGet(getToUriForSearchControllerMethod());
        this.toUriForDetailControllerMethod = null;
        break;
      }
      case DETAIL: {
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = addAndGet(getToUriForDetailControllerMethod());
        break;
      }
      default:
        this.toUriForCollectionControllerMethod = null;
        this.toUriForItemControllerMethod = null;
        this.toUriForSearchControllerMethod = null;
        this.toUriForDetailControllerMethod = null;
        break;
    }

    // TODO: Add needed fields and constructor

    // Build the ITD
    itdTypeDetails = builder.build();
  }


  private MethodMetadata getToUriForDetailControllerMethod() {
    // TODO Auto-generated method stub
    return null;
  }

  private MethodMetadata getToUriForSearchControllerMethod() {
    // TODO Auto-generated method stub
    return null;
  }

  private MethodMetadata getToUriForItemControllerMethod() {
    // TODO Auto-generated method stub
    return null;
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

    // Define parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // return method if already exists
    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // TODO create method body

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);

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
