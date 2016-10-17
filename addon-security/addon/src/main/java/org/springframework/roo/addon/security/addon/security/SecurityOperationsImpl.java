package org.springframework.roo.addon.security.addon.security;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.pushin.PushInOperations;
import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.DefaultImportMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides security installation services.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 1.0
 */
@Component
@Service
public class SecurityOperationsImpl implements SecurityOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(SecurityOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ProjectOperations projectOperations;
  private PathResolver pathResolver;
  private TypeLocationService typeLocationService;
  private TypeManagementService typeManagementService;
  private PushInOperations pushInOperations;

  private List<SecurityProvider> securityProviders = new ArrayList<SecurityProvider>();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void installSecurity(SecurityProvider type, Pom module) {

    Validate.notNull(type, "ERROR: You must provide a valid SecurityProvider to install.");

    // Delegates on the provided SecurityProvider to install Spring Security
    // on current project
    type.install(module);
  }

  @Override
  public void addPreAuthorizeAnnotation(JavaType klass, String methodName, String value) {

    Validate.notNull(klass,
        "ERROR: klass parameter is mandatory on 'addPreAuthorizeAnnotation' method");
    Validate.notNull(methodName,
        "ERROR: method parameter is mandatory on 'addPreAuthorizeAnnotation' method");
    Validate.notNull(value,
        "ERROR: value parameter is mandatory on 'addPreAuthorizeAnnotation' method");

    // Creating @PreAuthorize annotation
    AnnotationMetadataBuilder annotationPreAuthorize =
        new AnnotationMetadataBuilder(SpringJavaType.PRE_AUTHORIZE);
    annotationPreAuthorize.addStringAttribute("value", value);

    addSpringSecurityAnnotation(klass, methodName, annotationPreAuthorize);

  }

  @Override
  public void addPreFilterAnnotation(JavaType klass, String methodName, String value) {

    Validate.notNull(klass,
        "ERROR: klass parameter is mandatory on 'addPreFilterAnnotation' method");
    Validate.notNull(methodName,
        "ERROR: method parameter is mandatory on 'addPreFilterAnnotation' method");
    Validate.notNull(value,
        "ERROR: value parameter is mandatory on 'addPreFilterAnnotation' method");

    // Creating @PreFilter annotation
    AnnotationMetadataBuilder annotationPreAuthorize =
        new AnnotationMetadataBuilder(SpringJavaType.PRE_FILTER);
    annotationPreAuthorize.addStringAttribute("value", value);

    addSpringSecurityAnnotation(klass, methodName, annotationPreAuthorize);

  }

  @Override
  public void addPostFilterAnnotation(JavaType klass, String methodName, String value) {

    Validate.notNull(klass,
        "ERROR: klass parameter is mandatory on 'addPostFilterAnnotation' method");
    Validate.notNull(methodName,
        "ERROR: method parameter is mandatory on 'addPostFilterAnnotation' method");
    Validate.notNull(value,
        "ERROR: value parameter is mandatory on 'addPostFilterAnnotation' method");

    // Creating @PostFilter annotation
    AnnotationMetadataBuilder annotationPreAuthorize =
        new AnnotationMetadataBuilder(SpringJavaType.POST_FILTER);
    annotationPreAuthorize.addStringAttribute("value", value);

    addSpringSecurityAnnotation(klass, methodName, annotationPreAuthorize);

  }

  /**
   * This method will annotate the provided method with the provided security annotation.
   * 
   * @param klass Class that contains the method to annotate
   * @param methodName the method to annotate
   * @param annotation Spring Security annotation
   */
  private void addSpringSecurityAnnotation(JavaType klass, String methodName,
      AnnotationMetadataBuilder annotation) {

    Validate
        .notNull(klass, "ERROR: klass parameter is mandatory on 'addSecurityAnnotation' method");
    Validate.notNull(methodName,
        "ERROR: method parameter is mandatory on 'addSecurityAnnotation' method");
    Validate.notNull(annotation,
        "ERROR: method parameter is mandatory on 'addSecurityAnnotation' method");

    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(klass);
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(serviceDetails);

    // TODO: Analyze the possibility to decorate the generated code by other
    // Metadatas. By now, we need to make a push-in operation of the selected 
    // method to annotate the code generated by other metadata.
    List<Object> pushedElements =
        getPushInOperations().pushIn(klass.getPackage(), klass, methodName, false);

    if (pushedElements.isEmpty()) { // Means that method has been pushed before
      // TODO: This is a problem related with the code maintaineance. It's not possible to 
      // maintain methods in .java, so when the method is pushed, is not possible to add new annotations.
      // Is really necessary to analyze some alternative to decorate existing code generated by other
      // metadatas.
      LOGGER.log(Level.INFO,
          "ERROR: This method has been moved to the .java file so it's not possible "
              + "to maintain it. Include the Spring Security annotation manually.");
    } else {
      // Getting method to annotate
      for (Object pushedElement : pushedElements) {
        // Checking if the pushed element is a method
        if (pushedElement.getClass().isAssignableFrom(DefaultMethodMetadata.class)) {
          MethodMetadataBuilder method = new MethodMetadataBuilder((MethodMetadata) pushedElement);
          method.addAnnotation(annotation);
          cidBuilder.addMethod(method);
        } else if (pushedElement.getClass().isAssignableFrom(DefaultImportMetadata.class)) {
          // Checking if the pushed element is an import
          ImportMetadata importElement = (ImportMetadata) pushedElement;
          cidBuilder.add(importElement);
        }
      }
    }

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

  }

  @Override
  public List<SecurityProvider> getAllSecurityProviders() {
    if (securityProviders.isEmpty()) {
      // Get all Services implement SecurityProvider interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(SecurityProvider.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          SecurityProvider securityProvider = (SecurityProvider) this.context.getService(ref);
          securityProviders.add(securityProvider);
        }

        return securityProviders;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load SecurityProvider on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return securityProviders;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public TypeManagementService getTypeManagementService() {
    if (typeManagementService == null) {
      // Get all Services implement TypeManagementService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeManagementService = (TypeManagementService) this.context.getService(ref);
          return typeManagementService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeManagementService on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return typeManagementService;
    }
  }

  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return pathResolver;
    }
  }

  public PushInOperations getPushInOperations() {
    if (pushInOperations == null) {
      // Get all Services implement PushInOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PushInOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pushInOperations = (PushInOperations) this.context.getService(ref);
          return pushInOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PushInOperations on SecurityOperationsImpl.");
        return null;
      }
    } else {
      return pushInOperations;
    }
  }

  // FEATURE METHODS

  @Override
  public String getName() {
    return SECURITY_FEATURE_NAME;
  }

  @Override
  public boolean isInstalledInModule(String moduleName) {
    List<SecurityProvider> providers = getAllSecurityProviders();

    for (SecurityProvider provider : providers) {
      if (provider.isInstalledInModule(moduleName)) {
        return true;
      }
    }

    return false;
  }

}
