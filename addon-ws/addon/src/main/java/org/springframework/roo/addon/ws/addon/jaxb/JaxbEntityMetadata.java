package org.springframework.roo.addon.ws.addon.jaxb;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.ws.annotations.jaxb.RooJaxbEntity;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DeclaredMethodAnnotationDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.CommentStructure.CommentLocation;
import org.springframework.roo.classpath.details.comments.JavadocComment;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Metadata for {@link RooJaxbEntity}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class JaxbEntityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JaxbEntityMetadata.class);

  private static final String PROVIDES_TYPE_STRING = JaxbEntityMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final JavaPackage projectTopLevelPackage;
  private final JavaType entity;
  private final String pluralEntityName;
  private final MethodMetadata identifierAccessor;
  private final List<MethodMetadata> oneToManyGetters;
  private final List<MethodMetadata> manyToOneGetters;

  private MethodMetadata xmlIdentityInfoMethod;

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
   * @param projectTopLevelPackage the topLevelPackage
   * 		of the generated project.
   * @param entity the annotated entity
   * @param pluralEntityName the plural name of the entity
   * @param identifierAccessor the identifier accessor method
   * @param oneToManyGetters accessor methods of all oneToMany relation fields
   * @param manyToOneGetters accessor methods of all manyToOne relation fields
   */
  public JaxbEntityMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JavaPackage projectTopLevelPackage, final JavaType entity,
      final String pluralEntityName, final MethodMetadata identifierAccessor,
      final List<MethodMetadata> oneToManyGetters, final List<MethodMetadata> manyToOneGetters,
      Map<String, String> entityNames) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.projectTopLevelPackage = projectTopLevelPackage;
    this.entity = entity;
    this.pluralEntityName = pluralEntityName;
    this.identifierAccessor = identifierAccessor;
    this.oneToManyGetters = oneToManyGetters;
    this.manyToOneGetters = manyToOneGetters;

    // Declare precendence
    builder.setDeclarePrecedence(aspectName);

    // Include @XmlRootElement annotation
    AnnotationMetadataBuilder xmlRootElementAnnotation =
        new AnnotationMetadataBuilder(JavaType.XML_ROOT_ELEMENT);
    xmlRootElementAnnotation.addStringAttribute("name", entity.getSimpleTypeName().toLowerCase());
    xmlRootElementAnnotation.addStringAttribute(
        "namespace",
        String.format("http://ws.%s/", StringUtils.reverseDelimited(
            projectTopLevelPackage.getFullyQualifiedPackageName(), '.')));
    ensureGovernorIsAnnotated(xmlRootElementAnnotation);

    // Include annotations on @OneToMany getters
    for (MethodMetadata getter : oneToManyGetters) {

      // Getting the getter type name
      String getterTypeName =
          getter.getReturnType().getBaseType().getSimpleTypeName().toLowerCase();

      // Define @XmlIDREF annotation
      DeclaredMethodAnnotationDetails xmlIdRefAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(getter, new AnnotationMetadataBuilder(
              JavaType.XML_ID_REF).build());

      // Define @XmlElement annotation
      AnnotationMetadataBuilder xmlElementAnnotation =
          new AnnotationMetadataBuilder(JavaType.XML_ELEMENT);
      xmlElementAnnotation.addStringAttribute("name", getterTypeName);
      DeclaredMethodAnnotationDetails xmlElementAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(getter, xmlElementAnnotation.build());

      // Define @XmlElementWrapper annotation
      AnnotationMetadataBuilder xmlElementWrapperAnnotation =
          new AnnotationMetadataBuilder(JavaType.XML_ELEMENT_WRAPPER);
      xmlElementWrapperAnnotation.addStringAttribute("name", entityNames.get(getterTypeName));
      DeclaredMethodAnnotationDetails xmlElementWrapperAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(getter, xmlElementWrapperAnnotation.build());

      builder.addMethodAnnotation(xmlIdRefAnnotationInGetterMethod);
      builder.addMethodAnnotation(xmlElementAnnotationInGetterMethod);
      builder.addMethodAnnotation(xmlElementWrapperAnnotationInGetterMethod);
    }

    // Include annotations on @ManyToOne getters
    for (MethodMetadata getter : manyToOneGetters) {

      // Getting the getter type name
      String getterTypeName = getter.getReturnType().getSimpleTypeName().toLowerCase();

      // Define @XmlIDREF annotation
      DeclaredMethodAnnotationDetails xmlIdRefAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(getter, new AnnotationMetadataBuilder(
              JavaType.XML_ID_REF).build());

      // Define @XmlElement annotation
      AnnotationMetadataBuilder xmlElementAnnotation =
          new AnnotationMetadataBuilder(JavaType.XML_ELEMENT);
      xmlElementAnnotation.addStringAttribute("name", getterTypeName);
      DeclaredMethodAnnotationDetails xmlElementAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(getter, xmlElementAnnotation.build());

      builder.addMethodAnnotation(xmlIdRefAnnotationInGetterMethod);
      builder.addMethodAnnotation(xmlElementAnnotationInGetterMethod);
    }

    // Annotate the identifier accessor method and generate new one
    if (identifierAccessor != null) {
      // Define @XmlTransient annotation
      DeclaredMethodAnnotationDetails xmlTransientAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(identifierAccessor, new AnnotationMetadataBuilder(
              JavaType.XML_TRANSIENT).build());

      builder.addMethodAnnotation(xmlTransientAnnotationInGetterMethod);

      // Include getXmlIdentityInfo() method
      ensureGovernorHasMethod(new MethodMetadataBuilder(getXmlIdentityInfoMethod()));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }


  /**
   * This method returns the getXmlIdentityInfo() method.
   * 
   * @return MethodMetadata that contains the getXmlIdentityInfoMethod
   */
  public MethodMetadata getXmlIdentityInfoMethod() {

    // Check if already exists
    if (xmlIdentityInfoMethod != null) {
      return xmlIdentityInfoMethod;
    }

    // If not, generate a new one
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return getClass().getName() + ":" + getId();
    bodyBuilder.appendFormalLine(String.format("return getClass().getName() + \":\" + %s();",
        identifierAccessor.getMethodName()));

    MethodMetadataBuilder method =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
            new JavaSymbolName("getXmlIdentityInfo"), JavaType.STRING, bodyBuilder);
    method.addAnnotation(new AnnotationMetadataBuilder(JavaType.XML_ID));
    AnnotationMetadataBuilder xmlAttributeAnnotation =
        new AnnotationMetadataBuilder(JavaType.XML_ATTRIBUTE);
    xmlAttributeAnnotation.addStringAttribute("name", "id");
    method.addAnnotation(xmlAttributeAnnotation);

    CommentStructure comment = new CommentStructure();
    comment.addComment(new JavadocComment("Must return an unique ID across all entities"),
        CommentLocation.BEGINNING);
    method.setCommentStructure(comment);

    xmlIdentityInfoMethod = method.build();

    return xmlIdentityInfoMethod;
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

  public JavaPackage getProjectTopLevelPackage() {
    return projectTopLevelPackage;
  }

  public JavaType getEntity() {
    return entity;
  }

  public List<MethodMetadata> getOneToManyGetters() {
    return oneToManyGetters;
  }

  public List<MethodMetadata> getManyToOneGetters() {
    return manyToOneGetters;
  }

  public String getPluralEntityName() {
    return pluralEntityName;
  }

  public MethodMetadata getIdentifierAccessor() {
    return identifierAccessor;
  }

}
