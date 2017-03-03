package org.springframework.roo.addon.jpa.addon.audit;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implements {@link JpaAuditOperations} to be able to include
 * Jpa Audit support in generated projects
 * 
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class JpaAuditOperationsImpl implements JpaAuditOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JpaAuditOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  private static final Property SPRINGLETS_VERSION_PROPERTY = new Property("springlets.version",
      "1.2.0.RC1");
  private static final Dependency SPRINGLETS_DATA_JPA_STARTER = new Dependency("io.springlets",
      "springlets-boot-starter-data-jpa", "${springlets.version}");

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  @Override
  public boolean isJpaAuditSetupPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)
        && getProjectOperations().isFeatureInstalled(FeatureNames.JPA)
        && !getProjectOperations().isFeatureInstalled(AUDIT_FEATURE_NAME);
  }

  @Override
  public boolean isJpaAuditAddPossible() {
    return getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)
        && getProjectOperations().isFeatureInstalled(FeatureNames.AUDIT);
  }

  @Override
  public void setupJpaAudit(Pom module) {

    // Include Springlets Starter project dependencies and properties
    getProjectOperations().addProperty("", SPRINGLETS_VERSION_PROPERTY);

    // If current project is a multimodule project, include dependencies first
    // on dependencyManagement and then on current module
    getProjectOperations().addDependency(module.getModuleName(), SPRINGLETS_DATA_JPA_STARTER);

  }

  @Override
  public void addJpaAuditToEntity(JavaType entity, String createdDateColumn,
      String modifiedDateColumn, String createdByColumn, String modifiedByColumn) {

    // Getting entity details
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(entityDetails);

    // Add audit fields
    cidBuilder.addField(getCreatedDateField(entityDetails, createdDateColumn));
    cidBuilder.addField(getModifiedDateField(entityDetails, modifiedDateColumn));
    cidBuilder.addField(getCreatedByField(entityDetails, createdByColumn));
    cidBuilder.addField(getModifiedByField(entityDetails, modifiedByColumn));

    // Add @RooJpaAudit annotation if needed
    if (entityDetails.getAnnotation(RooJavaType.ROO_JPA_AUDIT) == null) {
      cidBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_JPA_AUDIT).build());
    }

    // Write changes on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  /**
   * Builds createdDate field for storing entity's created date 
   * 
   * @return FieldMetadataBuilder 
   */
  private FieldMetadataBuilder getCreatedDateField(ClassOrInterfaceTypeDetails entityDetails,
      String columnName) {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(columnName)) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", columnName);
      annotations.add(columnAnnotation);
    }

    // Add @CreatedDate
    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.CREATED_DATE);
    annotations.add(createdDateAnnotation);

    // Add @Temporal
    AnnotationMetadataBuilder temporalAnnotation =
        new AnnotationMetadataBuilder(JpaJavaType.TEMPORAL);
    temporalAnnotation.addEnumAttribute("value", new EnumDetails(JpaJavaType.TEMPORAL_TYPE,
        new JavaSymbolName("TIMESTAMP")));
    annotations.add(temporalAnnotation);

    // Add @DateTimeFormat
    AnnotationMetadataBuilder dateTimeFormatAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.DATE_TIME_FORMAT);
    dateTimeFormatAnnotation.addStringAttribute("style", "M-");
    annotations.add(dateTimeFormatAnnotation);

    // Create field
    FieldDetails fieldDetails =
        new FieldDetails(PhysicalTypeIdentifier.createIdentifier(entityDetails),
            JdkJavaType.CALENDAR, new JavaSymbolName("createdDate"));
    fieldDetails.setModifiers(Modifier.PRIVATE);
    fieldDetails.setAnnotations(annotations);
    FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails);
    return fieldBuilder;
  }

  /**
   * Builds modifiedDate field for storing entity's last modified date 
   * 
   * @return FieldMetadataBuilder
   */
  private FieldMetadataBuilder getModifiedDateField(ClassOrInterfaceTypeDetails entityDetails,
      String columnName) {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(columnName)) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", columnName);
      annotations.add(columnAnnotation);
    }

    // Add @LastModifiedDate
    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.LAST_MODIFIED_DATE);
    annotations.add(createdDateAnnotation);

    // Add @Temporal
    AnnotationMetadataBuilder temporalAnnotation =
        new AnnotationMetadataBuilder(JpaJavaType.TEMPORAL);
    temporalAnnotation.addEnumAttribute("value", new EnumDetails(JpaJavaType.TEMPORAL_TYPE,
        new JavaSymbolName("TIMESTAMP")));
    annotations.add(temporalAnnotation);

    // Add @DateTimeFormat
    AnnotationMetadataBuilder dateTimeFormatAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.DATE_TIME_FORMAT);
    dateTimeFormatAnnotation.addStringAttribute("style", "M-");
    annotations.add(dateTimeFormatAnnotation);

    // Create field
    FieldDetails fieldDetails =
        new FieldDetails(PhysicalTypeIdentifier.createIdentifier(entityDetails),
            JdkJavaType.CALENDAR, new JavaSymbolName("modifiedDate"));
    fieldDetails.setModifiers(Modifier.PRIVATE);
    fieldDetails.setAnnotations(annotations);
    FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails);
    return fieldBuilder;
  }

  /**
   * Builds createdBy field for storing user who creates entity registers
   * 
   * @return FieldMetadataBuilder
   */
  private FieldMetadataBuilder getCreatedByField(ClassOrInterfaceTypeDetails entityDetails,
      String columnName) {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(columnName)) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", columnName);
      annotations.add(columnAnnotation);
    }

    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.CREATED_BY);
    annotations.add(createdDateAnnotation);

    // Create field
    FieldDetails fieldDetails =
        new FieldDetails(PhysicalTypeIdentifier.createIdentifier(entityDetails), JavaType.STRING,
            new JavaSymbolName("createdBy"));
    fieldDetails.setModifiers(Modifier.PRIVATE);
    fieldDetails.setAnnotations(annotations);
    FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails);
    return fieldBuilder;
  }

  /**
   * Builds modifiedBy field for storing user who last modifies entity registers
   * 
   * @return FieldMetadataBuilder
   */
  private FieldMetadataBuilder getModifiedByField(ClassOrInterfaceTypeDetails entityDetails,
      String columnName) {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(columnName)) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", columnName);
      annotations.add(columnAnnotation);
    }

    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.LAST_MODIFIED_BY);
    annotations.add(createdDateAnnotation);

    // Create field
    FieldDetails fieldDetails =
        new FieldDetails(PhysicalTypeIdentifier.createIdentifier(entityDetails), JavaType.STRING,
            new JavaSymbolName("modifiedBy"));
    fieldDetails.setModifiers(Modifier.PRIVATE);
    fieldDetails.setAnnotations(annotations);
    FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails);
    return fieldBuilder;
  }


  // OSGi Services

  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  // FEATURE OPERATIONS

  @Override
  public String getName() {
    return AUDIT_FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    boolean isInstalledInModule = false;
    Pom module = getProjectOperations().getPomFromModuleName(moduleName);
    Set<Dependency> starter = module.getDependenciesExcludingVersion(SPRINGLETS_DATA_JPA_STARTER);

    if (!starter.isEmpty()) {
      isInstalledInModule = true;
    }

    return isInstalledInModule;
  }
}
