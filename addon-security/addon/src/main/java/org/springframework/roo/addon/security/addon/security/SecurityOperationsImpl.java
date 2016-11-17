package org.springframework.roo.addon.security.addon.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.pushin.PushInOperations;
import org.springframework.roo.addon.security.addon.security.providers.SecurityProvider;
import org.springframework.roo.addon.security.annotations.RooSecurityAuthorization;
import org.springframework.roo.addon.security.annotations.RooSecurityFilter;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.project.Dependency;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

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

  private static final Dependency SPRING_SECURITY_CORE = new Dependency(
      "org.springframework.security", "spring-security-core", null);

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

  public void generateFilterAnnotations(JavaType klass, String methodName, String roles,
      String usernames, String when) {

    // Get methods to annotate.
    // With the last parameter to false, we avoid that push in action occurs.
    List<Object> pushedElements =
        getPushInOperations().pushIn(klass.getPackage(), klass, methodName, false);

    List<AnnotationAttributeValue<?>> rooSecurityFiltersToAdd =
        new ArrayList<AnnotationAttributeValue<?>>();

    for (Object pushedElement : pushedElements) {
      if (pushedElement instanceof DefaultMethodMetadata) {
        DefaultMethodMetadata method = (DefaultMethodMetadata) pushedElement;

        // Get parameters
        List<AnnotationAttributeValue<?>> lstParamTypes =
            new ArrayList<AnnotationAttributeValue<?>>();
        List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();
        Iterator<AnnotatedJavaType> iterParamTypes = parameterTypes.iterator();
        while (iterParamTypes.hasNext()) {
          ClassAttributeValue parameterAttributeValue =
              new ClassAttributeValue(new JavaSymbolName("value"), iterParamTypes.next()
                  .getJavaType());
          lstParamTypes.add(parameterAttributeValue);
        }

        // Generate new annotations @RooSecurityFilter
        NestedAnnotationAttributeValue newFilter =
            new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
                getRooSecurityFilterAnnotation(method.getMethodName().getSymbolName(),
                    lstParamTypes, roles, usernames, when).build());
        rooSecurityFiltersToAdd.add(newFilter);
      }
    }

    // Get actual values of @RooSecurityFilters
    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(klass);
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(serviceDetails);

    // Check annotation @RooSecurityFilters to delete defined annotations
    // that will be redefined
    AnnotationMetadata annotationFilters =
        serviceDetails.getAnnotation(RooJavaType.ROO_SECURITY_FILTERS);
    AnnotationMetadataBuilder annotationFiltersMetadataBuilder;

    if (annotationFilters != null) {

      // Getting filters from annotation
      AnnotationAttributeValue<?> attributeFilters = annotationFilters.getAttribute("filters");
      List<?> values = (List<?>) attributeFilters.getValue();
      if (values != null && !values.isEmpty()) {
        Iterator<?> valuesIt = values.iterator();

        while (valuesIt.hasNext()) {
          NestedAnnotationAttributeValue filterAnnotation =
              (NestedAnnotationAttributeValue) valuesIt.next();
          if (checkRooSecurityFilterMaintainAnnotation(rooSecurityFiltersToAdd, filterAnnotation)) {

            // Maintain annotation if 'method', 'parameters' or 'when' are different
            rooSecurityFiltersToAdd.add(filterAnnotation);

          }
        }
      }
      annotationFiltersMetadataBuilder = new AnnotationMetadataBuilder(annotationFilters);

      // remove annotation
      cidBuilder.removeAnnotation(RooJavaType.ROO_SECURITY_FILTERS);

    } else {

      // Doesn't exist @RooSecurityFilters, create it
      annotationFiltersMetadataBuilder =
          new AnnotationMetadataBuilder(RooJavaType.ROO_SECURITY_FILTERS);
    }


    // Add filters attribute
    ArrayAttributeValue<AnnotationAttributeValue<?>> newFilters =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("filters"),
            rooSecurityFiltersToAdd);
    annotationFiltersMetadataBuilder.addAttribute(newFilters);

    // Include new @RooSecurityFilters annotation
    cidBuilder.addAnnotation(annotationFiltersMetadataBuilder);

    // Write on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    // Add Spring Security dependency
    getProjectOperations().addDependency(klass.getModule(), SPRING_SECURITY_CORE, false);

  }

  /**
   * Check if {@link RooSecurityFilter} annotation should be kept
   * or should be replaced because is defined in the annotations list to add.
   *
   * @param rooSecurityFiltersToAdd Annotations list to add
   * @param filterAnnotation Annotation to check
   * @return
   */
  private boolean checkRooSecurityFilterMaintainAnnotation(
      List<AnnotationAttributeValue<?>> rooSecurityFiltersToAdd,
      NestedAnnotationAttributeValue filterAnnotation) {

    boolean maintainAnnotation = true;

    String annotationMethod =
        (String) filterAnnotation.getValue().getAttribute("method").getValue();
    List<?> annotationParameters =
        (List<?>) filterAnnotation.getValue().getAttribute("parameters").getValue();
    String annotationWhen = (String) filterAnnotation.getValue().getAttribute("when").getValue();

    Iterator<AnnotationAttributeValue<?>> iterParamTypes = rooSecurityFiltersToAdd.iterator();
    while (iterParamTypes.hasNext()) {
      NestedAnnotationAttributeValue rooSecurityFilterToAdd =
          (NestedAnnotationAttributeValue) iterParamTypes.next();
      String annotationMethodToAdd =
          (String) rooSecurityFilterToAdd.getValue().getAttribute("method").getValue();
      List<?> annotationParametersToAdd =
          (List<?>) rooSecurityFilterToAdd.getValue().getAttribute("parameters").getValue();
      String annotationWhenToAdd =
          (String) rooSecurityFilterToAdd.getValue().getAttribute("when").getValue();

      boolean parametersAreEquals = true;
      if (annotationParametersToAdd.size() != annotationParameters.size()) {
        parametersAreEquals = false;
      } else {
        for (int i = 0; i < annotationParametersToAdd.size(); i++) {
          ClassAttributeValue classAnnotationParametersToAdd =
              (ClassAttributeValue) annotationParametersToAdd.get(i);
          ClassAttributeValue classAnnotationParameters =
              (ClassAttributeValue) annotationParameters.get(i);
          if (!classAnnotationParametersToAdd.getValue().getSimpleTypeName()
              .equals(classAnnotationParameters.getValue().getSimpleTypeName())) {
            parametersAreEquals = false;
            break;
          }
        }
      }

      if (annotationMethodToAdd.equals(annotationMethod)
          && annotationWhenToAdd.equals(annotationWhen) && parametersAreEquals) {
        maintainAnnotation = false;
        break;
      }
    }

    return maintainAnnotation;
  }

  /**
   * This method provides {@link RooSecurityFilter} annotation with all the necessary
   * attributes
   *
   * @param method Method to add the annotation
   * @param lstParamTypes Parameter types of the method to add the annotation
   * @param roles Roles to apply by the filter
   * @param usernames Usernames apply by the filter
   * @param when Indicate the type of filter 'PRE' (@PreFilter) or 'POST' (@PostFilter)
   * @return the annotation created
   */
  private AnnotationMetadataBuilder getRooSecurityFilterAnnotation(final String method,
      final List<AnnotationAttributeValue<?>> lstParamTypes, final String roles,
      final String usernames, final String when) {
    final List<AnnotationAttributeValue<?>> attributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("method"), method));
    ArrayAttributeValue<AnnotationAttributeValue<?>> newParameters =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("parameters"),
            lstParamTypes);
    attributes.add(newParameters);
    if (roles != null) {
      attributes.add(new StringAttributeValue(new JavaSymbolName("roles"), roles));
    }
    if (usernames != null) {
      attributes.add(new StringAttributeValue(new JavaSymbolName("usernames"), usernames));
    }
    attributes.add(new StringAttributeValue(new JavaSymbolName("when"), when));
    return new AnnotationMetadataBuilder(RooJavaType.ROO_SECURITY_FILTER, attributes);
  }

  @Override
  public void generateAuthorizeAnnotations(JavaType klass, String methodName, String roles,
      String usernames) {

    Validate.notNull(klass,
        "ERROR: klass parameter is mandatory on 'generateAuthorizeAnnotations' method");
    Validate.notNull(methodName,
        "ERROR: method parameter is mandatory on 'generateAuthorizeAnnotations' method");

    // Get methods to annotate.
    // With the last parameter to false, we avoid that push in action occurs.
    List<Object> pushedElements =
        getPushInOperations().pushIn(klass.getPackage(), klass, methodName, false);

    List<AnnotationAttributeValue<?>> rooSecurityAuthorizationsToAdd =
        new ArrayList<AnnotationAttributeValue<?>>();

    for (Object pushedElement : pushedElements) {
      if (pushedElement instanceof DefaultMethodMetadata) {
        DefaultMethodMetadata method = (DefaultMethodMetadata) pushedElement;

        // Get parameters
        List<AnnotationAttributeValue<?>> lstParamTypes =
            new ArrayList<AnnotationAttributeValue<?>>();
        List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();
        Iterator<AnnotatedJavaType> iterParamTypes = parameterTypes.iterator();
        while (iterParamTypes.hasNext()) {
          ClassAttributeValue parameterAttributeValue =
              new ClassAttributeValue(new JavaSymbolName("value"), iterParamTypes.next()
                  .getJavaType());
          lstParamTypes.add(parameterAttributeValue);
        }

        // Generate new annotations @RooSecurityAuthorization
        NestedAnnotationAttributeValue newFilter =
            new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
                getRooSecurityAuthorizationsAnnotation(method.getMethodName().getSymbolName(),
                    lstParamTypes, roles, usernames).build());
        rooSecurityAuthorizationsToAdd.add(newFilter);
      }
    }

    // Get actual values of @RooSecurityAuthorizations
    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(klass);
    ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(serviceDetails);

    // Check annotation @RooSecurityAuthorizations to delete defined annotations
    // that will be redefined
    AnnotationMetadata annotationAuthorizations =
        serviceDetails.getAnnotation(RooJavaType.ROO_SECURITY_AUTHORIZATIONS);
    AnnotationMetadataBuilder annotationAuthorizationsMetadataBuilder;

    if (annotationAuthorizations != null) {

      // Getting authorizations from annotation
      AnnotationAttributeValue<?> attributeAuthorizations =
          annotationAuthorizations.getAttribute("authorizations");
      List<?> values = (List<?>) attributeAuthorizations.getValue();
      if (values != null && !values.isEmpty()) {
        Iterator<?> valuesIt = values.iterator();

        while (valuesIt.hasNext()) {
          NestedAnnotationAttributeValue authorizationAnnotation =
              (NestedAnnotationAttributeValue) valuesIt.next();
          if (checkRooSecurityAuthorizationMaintainAnnotation(rooSecurityAuthorizationsToAdd,
              authorizationAnnotation)) {

            // Maintain annotation if 'method' or 'parameters' are different
            rooSecurityAuthorizationsToAdd.add(authorizationAnnotation);

          }
        }
      }
      annotationAuthorizationsMetadataBuilder =
          new AnnotationMetadataBuilder(annotationAuthorizations);

      // remove annotation
      cidBuilder.removeAnnotation(RooJavaType.ROO_SECURITY_AUTHORIZATIONS);

    } else {

      // Doesn't exist @RooSecurityAuthorizations, create it
      annotationAuthorizationsMetadataBuilder =
          new AnnotationMetadataBuilder(RooJavaType.ROO_SECURITY_AUTHORIZATIONS);
    }

    // Add authorizations attribute
    ArrayAttributeValue<AnnotationAttributeValue<?>> newAuthorizations =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("authorizations"),
            rooSecurityAuthorizationsToAdd);
    annotationAuthorizationsMetadataBuilder.addAttribute(newAuthorizations);

    // Include new @RooSecurityAuthorizations annotation
    cidBuilder.addAnnotation(annotationAuthorizationsMetadataBuilder);

    // Write on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    // Add Spring Security dependency
    getProjectOperations().addDependency(klass.getModule(), SPRING_SECURITY_CORE, false);
  }

  /**
   * This method provides {@link RooSecurityAuthorization} annotation with all the necessary
   * attributes
   *
   * @param method Method to add the annotation
   * @param lstParamTypes Parameter types of the method to add the annotation
   * @param roles Roles to apply by the filter
   * @param usernames Usernames apply by the filter
   * @return the annotation created
   */
  private AnnotationMetadataBuilder getRooSecurityAuthorizationsAnnotation(final String method,
      final List<AnnotationAttributeValue<?>> lstParamTypes, final String roles,
      final String usernames) {
    final List<AnnotationAttributeValue<?>> attributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("method"), method));
    ArrayAttributeValue<AnnotationAttributeValue<?>> newParameters =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("parameters"),
            lstParamTypes);
    attributes.add(newParameters);
    if (roles != null) {
      attributes.add(new StringAttributeValue(new JavaSymbolName("roles"), roles));
    }
    if (usernames != null) {
      attributes.add(new StringAttributeValue(new JavaSymbolName("usernames"), usernames));
    }
    return new AnnotationMetadataBuilder(RooJavaType.ROO_SECURITY_AUTHORIZATION, attributes);
  }

  /**
   * Check if {@link RooSecurityAuthorization} annotation should be kept
   * or should be replaced because is defined in the annotations list to add.
   *
   * @param rooSecurityAuthorizationsToAdd Annotations list to add
   * @param authorizationAnnotation Annotation to check
   * @return
   */
  private boolean checkRooSecurityAuthorizationMaintainAnnotation(
      List<AnnotationAttributeValue<?>> rooSecurityAuthorizationsToAdd,
      NestedAnnotationAttributeValue authorizationAnnotation) {

    boolean maintainAnnotation = true;

    String annotationMethod =
        (String) authorizationAnnotation.getValue().getAttribute("method").getValue();
    List<?> annotationParameters =
        (List<?>) authorizationAnnotation.getValue().getAttribute("parameters").getValue();

    Iterator<AnnotationAttributeValue<?>> iterParamTypes =
        rooSecurityAuthorizationsToAdd.iterator();
    while (iterParamTypes.hasNext()) {
      NestedAnnotationAttributeValue rooSecurityAuthorizationToAdd =
          (NestedAnnotationAttributeValue) iterParamTypes.next();
      String annotationMethodToAdd =
          (String) rooSecurityAuthorizationToAdd.getValue().getAttribute("method").getValue();
      List<?> annotationParametersToAdd =
          (List<?>) rooSecurityAuthorizationToAdd.getValue().getAttribute("parameters").getValue();

      boolean parametersAreEquals = true;
      if (annotationParametersToAdd.size() != annotationParameters.size()) {
        parametersAreEquals = false;
      } else {
        for (int i = 0; i < annotationParametersToAdd.size(); i++) {
          ClassAttributeValue classAnnotationParametersToAdd =
              (ClassAttributeValue) annotationParametersToAdd.get(i);
          ClassAttributeValue classAnnotationParameters =
              (ClassAttributeValue) annotationParameters.get(i);
          if (!classAnnotationParametersToAdd.getValue().getSimpleTypeName()
              .equals(classAnnotationParameters.getValue().getSimpleTypeName())) {
            parametersAreEquals = false;
            break;
          }
        }
      }

      if (annotationMethodToAdd.equals(annotationMethod) && parametersAreEquals) {
        maintainAnnotation = false;
        break;
      }
    }

    return maintainAnnotation;
  }


  @Override
  public String getSpringSecurityAnnotationValue(String roles, String usernames) {

    String value = "";

    // Including roles
    if (StringUtils.isNotEmpty(roles)) {

      // First of all, obtain the comma separated list
      // that contains all roles
      String[] rolesList = roles.split(",");

      // Now, check if there's more than one role
      if (rolesList.length > 1) {
        // create the hasAnyRole expression
        value = "hasAnyRole(";
      } else {
        // create the hasRole expression
        value = "hasRole(";
      }

      for (String role : rolesList) {
        value = value.concat("'").concat(role).concat("'").concat(",");
      }
      value = value.substring(0, value.length() - 1).concat(")");
    }

    // Including usernames
    if (StringUtils.isNotEmpty(usernames)) {

      // First of all, obtain the comma separated list
      // that contains all usernames
      String[] usernamesList = usernames.split(",");

      // Check if also exist some role added previously
      if (StringUtils.isNotEmpty(value) && usernamesList.length > 0) {
        value = value.concat(" or");
      }

      // Create (#username == principal.username) expression
      for (String username : usernamesList) {
        value = value.concat(" (#").concat(username).concat(" == principal.username) or");
      }

      // Removing last extra or
      value = value.substring(0, value.length() - 3);
    }

    return value.trim();
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
