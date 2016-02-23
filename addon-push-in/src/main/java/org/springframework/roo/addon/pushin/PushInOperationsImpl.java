package org.springframework.roo.addon.pushin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.ImportRegistrationResolverImpl;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Operations for the 'push-in' add-on.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class PushInOperationsImpl implements PushInOperations {

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  private static final Logger LOGGER = HandlerUtils.getLogger(PushInOperationsImpl.class);

  private ProjectOperations projectOperations;
  private TypeLocationService typeLocationService;
  private MemberDetailsScanner memberDetailsScanner;
  private TypeManagementService typeManagementService;
  private PathResolver pathResolver;

  /** {@inheritdoc] */
  public void pushInClass(JavaType klass) {
    // Check if current klass exists
    Validate
        .notNull(klass, "ERROR: You must specify a valid class to continue with push-in action");

    // Getting class details
    ClassOrInterfaceTypeDetails classDetails = getTypeLocationService().getTypeDetails(klass);
    Validate
        .notNull(klass, "ERROR: You must specify a valid class to continue with push-in action");

    // Getting member details
    MemberDetails memberDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);

    // Getting all declared methods (including declared on ITDs
    // and .java files)
    List<MethodMetadata> allDeclaredMethods = memberDetails.getMethods();
    // Getting all declared fields (including declared on ITDs
    // and .java files)
    List<FieldMetadata> allDeclaredFields = memberDetails.getFields();
    // Getting all declared annotations (including declared on ITDs
    // and .java files)
    List<AnnotationMetadata> allDeclaredAnnotations = new ArrayList<AnnotationMetadata>();
    for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
      allDeclaredAnnotations.addAll(memberHoldingTypeDetails.getAnnotations());
    }

    // TODO: Getting all imports registered on .aj file to move to .java file

    // Getting current class .java file metadata ID
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(klass,
            getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));

    // Getting detailsBuilder
    ClassOrInterfaceTypeDetailsBuilder detailsBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(classDetails);

    // Checking if is necessary to make push-in for all declared methods
    for (MethodMetadata method : allDeclaredMethods) {
      // If method exists on .aj file, add it!
      if (!method.getDeclaredByMetadataId().equals(declaredByMetadataId)) {
        // Add method to .java file
        detailsBuilder.addMethod(getNewMethod(declaredByMetadataId, method));
      }
    }

    // Checking if is necessary to make push-in for all declared fields
    for (FieldMetadata field : allDeclaredFields) {
      // If field exists on .aj file, add it!
      if (!field.getDeclaredByMetadataId().equals(declaredByMetadataId)) {
        // Add field to .java file
        detailsBuilder.addField(getNewField(declaredByMetadataId, field));
      }
    }

    // Checking if is necessary to make push-in for all declared annotations
    for (AnnotationMetadata annotation : allDeclaredAnnotations) {
      // Check if current annotation exists on .java file
      classDetails = getTypeLocationService().getTypeDetails(detailsBuilder.build().getType());
      List<AnnotationMetadata> javaDeclaredAnnotations = classDetails.getAnnotations();
      boolean annotationExists = false;
      for (AnnotationMetadata javaAnnotation : javaDeclaredAnnotations) {
        if (javaAnnotation.getAnnotationType().getFullyQualifiedTypeName()
            .equals(annotation.getAnnotationType().getFullyQualifiedTypeName())) {
          annotationExists = true;
        }
      }

      // If not exists, add it!
      if (!annotationExists) {
        // Add annotation to .java file
        detailsBuilder.addAnnotation(annotation);
      }

    }

    // Updating .java file
    getTypeManagementService().createOrUpdateTypeOnDisk(detailsBuilder.build());

  }

  /** {@inheritdoc] */
  public void pushInAll() {
    // Getting all JavaTypes on current project
    Collection<JavaType> allDeclaredTypes =
        getTypeLocationService().getTypesForModule(getProjectOperations().getFocusedModule());
    for (JavaType declaredType : allDeclaredTypes) {
      // Push-in all content from .aj files to .java files
      pushInClass(declaredType);
    }

  }

  /** {@inheritdoc] */
  public boolean isPushInCommandAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  /**
   * This method generates new method instance using an existing
   * methodMetadata
   * 
   * @param declaredByMetadataId
   * @param method
   * 
   * @return
   */
  private MethodMetadata getNewMethod(String declaredByMetadataId, MethodMetadata method) {

    // Create bodyBuilder
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine(method.getBody());

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    // based on existing method
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(declaredByMetadataId, method.getModifier(),
            method.getMethodName(), method.getReturnType(), method.getParameterTypes(),
            method.getParameterNames(), bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * This method generates new field instance using an existing FieldMetadata
   * 
   * @param declaredByMetadataId
   * @param field
   * 
   * @return
   */
  private FieldMetadata getNewField(String declaredByMetadataId, FieldMetadata field) {

    // Use the FieldMetadataBuilder for easy creation of FieldMetadata
    // based on existing field
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(declaredByMetadataId, field.getModifier(), field.getFieldName(),
            field.getFieldType(), field.getFieldInitializer());
    fieldBuilder.setAnnotations(field.getAnnotations());

    return fieldBuilder.build();
  }

  /**
   * Method to obtain projectOperation service implementation
   * 
   * @return
   */
  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) context.getService(ref);
          return projectOperations;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on PushInOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  /**
   * Method to obtain typeLocationService service implementation
   * 
   * @return
   */
  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) context.getService(ref);
          return typeLocationService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on PushInOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  /**
   * Method to obtain memberDetailsScanner service implementation
   * 
   * @return
   */
  public MemberDetailsScanner getMemberDetailsScanner() {
    if (memberDetailsScanner == null) {
      // Get all Services implement MemberDetailsScanner interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          memberDetailsScanner = (MemberDetailsScanner) context.getService(ref);
          return memberDetailsScanner;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MemberDetailsScanner on PushInOperationsImpl.");
        return null;
      }
    } else {
      return memberDetailsScanner;
    }
  }

  /**
   * Method to obtain typeManagementService service implementation
   * 
   * @return
   */
  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) context.getService(ref);
          return typeManagementService;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on PushInOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  /**
   * Method to obtain pathResolver service implementation
   * 
   * @return
   */
  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) context.getService(ref);
          return pathResolver;
        }
        return null;
      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on PushInOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

}
