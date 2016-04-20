package org.springframework.roo.addon.web.mvc.controller.addon.finder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerOperations;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.controller.addon.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link WebFinderOperations}
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @author Paula Navarro
 * @since 1.2.0
 */
@Component
@Service
public class WebFinderOperationsImpl implements WebFinderOperations {

  @Reference
  private ControllerOperations controllerOperations;
  @Reference
  private MetadataService metadataService;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private ProjectOperations projectOperations;


  @Override
  public void addFinders(JavaType controller, List<String> finderMethods,
      ControllerMVCResponseService responseType) {
    Validate.notNull(controller, "Controller type required");
    Validate.notNull(responseType, "Response type required");

    if (typeLocationService.getTypeDetails(controller).getAnnotation(RooJavaType.ROO_CONTROLLER) == null) {
      throw new IllegalArgumentException(String.format(
          "ERROR: Controller  %s is not annotated with @RooController",
          controller.getFullyQualifiedTypeName()));
    }

    if (!responseType.hasResponseType(controller)) {
      throw new IllegalArgumentException(String.format(
          "ERROR: Controller %s does not support %s response",
          controller.getFullyQualifiedTypeName(), responseType.getName()));
    }

    if (!finderMethods.isEmpty()) {
      responseType.addFinders(controller, finderMethods);
    }
  }

  public void annotateType(final JavaType controllerType, final JavaType entityType) {

    final String id = typeLocationService.getPhysicalTypeIdentifier(controllerType);
    if (id == null) {
      throw new IllegalArgumentException("Cannot locate source for '"
          + controllerType.getFullyQualifiedTypeName() + "'");
    }

    // Obtain the physical type and itd mutable details
    final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
    Validate.notNull(ptm, "Java source code unavailable for type %s",
        PhysicalTypeIdentifier.getFriendlyName(id));
    final WebScaffoldAnnotationValues webScaffoldAnnotationValues =
        new WebScaffoldAnnotationValues(ptm);
    if (!webScaffoldAnnotationValues.isAnnotationFound()
        || !webScaffoldAnnotationValues.getFormBackingObject().equals(entityType)) {
      throw new IllegalArgumentException("Aborting, this controller type does not manage the "
          + entityType.getSimpleTypeName() + " form backing type.");
    }

    final PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
    Validate.notNull(ptd, "Java source code details unavailable for type %s",
        PhysicalTypeIdentifier.getFriendlyName(id));
    final ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) ptd;
    if (null == MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(),
        RooJavaType.ROO_WEB_FINDER)) {
      final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(cid);
      cidBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_FINDER));
      typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }
  }

  public boolean isWebFinderInstallationPossible() {
    return projectOperations.isFeatureInstalled(FeatureNames.MVC);
  }
}
