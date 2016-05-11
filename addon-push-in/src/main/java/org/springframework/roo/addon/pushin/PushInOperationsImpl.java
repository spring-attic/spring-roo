package org.springframework.roo.addon.pushin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
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

  @Override
  public boolean isPushInCommandAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public void pushInAll(boolean force) {

    // Getting all JavaTypes on current project
    for (String moduleName : getProjectOperations().getModuleNames()) {
      Collection<JavaType> allDeclaredTypes =
          getTypeLocationService().getTypesForModule(
              getProjectOperations().getPomFromModuleName(moduleName));

      for (JavaType declaredType : allDeclaredTypes) {
        // Push-in all content from .aj files to .java files
        pushInClass(declaredType, force);
      }
    }

    if (!force) {
      LOGGER
          .log(
              Level.INFO,
              "All these changes will be applied. Execute your previous push-in command using --force parameter to apply them.");
    }
  }

  @Override
  public void pushIn(JavaPackage specifiedPackage, JavaType klass, String method) {

    // Getting all JavaTypes on current project
    Collection<JavaType> allDeclaredTypes = new ArrayList<JavaType>();

    for (String moduleName : getProjectOperations().getModuleNames()) {
      allDeclaredTypes.addAll(getTypeLocationService().getTypesForModule(
          getProjectOperations().getPomFromModuleName(moduleName)));
    }

    // Checking current class
    if (klass != null) {

      ClassOrInterfaceTypeDetails classDetails = getTypeLocationService().getTypeDetails(klass);
      Validate.notNull(
          classDetails,
          String.format("ERROR: Provided class '%s' doesn't exist on current project.",
              klass.getSimpleTypeName()));

      // If --class parameter is provided, --package will be ignored
      specifiedPackage = klass.getPackage();

      // If --method is provided, method should exist on the provided class
      if (method != null) {

        boolean methodExists = false;
        MemberDetails classMemberDetails =
            getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);
        for (MethodMetadata classMethod : classMemberDetails.getMethods()) {
          if (methodMatch(classMethod.getMethodName().getSymbolName(), method)) {
            pushInMethod(klass, classMethod);
            methodExists = true;
          }
        }

        Validate.isTrue(methodExists, String.format(
            "ERROR: No methods found on class '%s' that matches with '%s' expression.",
            klass.getSimpleTypeName(), method));

      } else {
        // If method is not specified, push-in entire class elements
        pushInClass(klass, true);
      }

    } else if (specifiedPackage != null && method != null) {
      // Check method on specified package
      boolean methodExists = false;

      for (JavaType declaredType : allDeclaredTypes) {
        // Check only classes on specified package
        if (declaredType.getPackage().equals(specifiedPackage)) {
          ClassOrInterfaceTypeDetails classDetails =
              getTypeLocationService().getTypeDetails(declaredType);
          MemberDetails classMemberDetails =
              getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);
          for (MethodMetadata classMethod : classMemberDetails.getMethods()) {
            if (methodMatch(classMethod.getMethodName().getSymbolName(), method)) {
              pushInMethod(declaredType, classMethod);
              methodExists = true;
            }
          }
        }
      }

      Validate.isTrue(methodExists, String.format(
          "ERROR: No methods found on package '%s' that matches with '%s' expression.",
          specifiedPackage.getFullyQualifiedPackageName(), method));

    } else if (method != null) {
      // Check that exists some method that match with provided method
      boolean methodExists = false;

      for (JavaType declaredType : allDeclaredTypes) {
        ClassOrInterfaceTypeDetails classDetails =
            getTypeLocationService().getTypeDetails(declaredType);
        MemberDetails classMemberDetails =
            getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);
        for (MethodMetadata classMethod : classMemberDetails.getMethods()) {
          if (methodMatch(classMethod.getMethodName().getSymbolName(), method)) {
            pushInMethod(declaredType, classMethod);
            methodExists = true;
          }
        }
      }

      Validate.isTrue(methodExists, String.format(
          "ERROR: No methods found on entire project that matches with '%s' expression.", method));

    } else if (specifiedPackage != null) {
      for (JavaType declaredType : allDeclaredTypes) {
        if (declaredType.getPackage().equals(specifiedPackage)) {
          pushInClass(declaredType, true);
        }
      }
    } else {
      LOGGER.log(Level.WARNING, "ERROR: You must specify at least one parameter. ");
      return;
    }
  }

  /**
   * Makes push-in of all items defined on a provided class
   * 
   * @param klass
   * @param force
   */
  public void pushInClass(JavaType klass, boolean force) {
    // Check if current klass exists
    Validate
        .notNull(klass, "ERROR: You must specify a valid class to continue with push-in action");

    // Getting class details
    ClassOrInterfaceTypeDetails classDetails = getTypeLocationService().getTypeDetails(klass);
    Validate
        .notNull(klass, "ERROR: You must specify a valid class to continue with push-in action");

    // String builder where changes will be registered
    StringBuilder changesToApply = new StringBuilder();

    // Getting member details
    MemberDetails memberDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);

    // Check if the provided class is a test to be able to select valid class path
    Path path =
        classDetails.getAnnotation(RooJavaType.ROO_UNIT_TEST) == null ? Path.SRC_MAIN_JAVA
            : Path.SRC_TEST_JAVA;
    // Getting current class .java file metadata ID
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(klass,
            getPathResolver().getPath(klass.getModule(), path));

    // Getting detailsBuilder
    ClassOrInterfaceTypeDetailsBuilder detailsBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(classDetails);

    // Getting all details 
    for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {

      // Prevent that details from inheritance classes could be include on this .java file 
      if (!memberHoldingTypeDetails.getType().equals(classDetails.getType())) {
        continue;
      }

      // Getting all declared methods (including declared on ITDs
      // and .java files)
      List<MethodMetadata> allDeclaredMethods = memberHoldingTypeDetails.getMethods();

      // Checking if is necessary to make push-in for all declared methods
      for (MethodMetadata method : allDeclaredMethods) {
        // If method exists on .aj file, add it!
        if (method.getDeclaredByMetadataId().split("\\?").length > 1
            && method.getDeclaredByMetadataId().split("\\?")[1].equals(klass
                .getFullyQualifiedTypeName())
            && !method.getDeclaredByMetadataId().equals(declaredByMetadataId)) {
          // Add method to .java file
          detailsBuilder.addMethod(getNewMethod(declaredByMetadataId, method));
          changesToApply.append(String.format("Method '%s' will be pushed on '%s.java' class. \n",
              method.getMethodName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all declared fields (including declared on ITDs
      // and .java files)
      List<? extends FieldMetadata> allDeclaredFields =
          memberHoldingTypeDetails.getDeclaredFields();

      // Checking if is necessary to make push-in for all declared fields
      for (FieldMetadata field : allDeclaredFields) {
        // If field exists on .aj file, add it!
        if (field.getDeclaredByMetadataId().split("\\?").length > 1
            && field.getDeclaredByMetadataId().split("\\?")[1].equals(klass
                .getFullyQualifiedTypeName())
            && !field.getDeclaredByMetadataId().equals(declaredByMetadataId)) {
          // Add field to .java file
          detailsBuilder.addField(getNewField(declaredByMetadataId, field));
          changesToApply.append(String.format("Field '%s' will be pushed on '%s.java' class. \n",
              field.getFieldName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all declared constructors (including declared on ITDs and .java files)
      List<? extends ConstructorMetadata> allDeclaredConstructors =
          memberHoldingTypeDetails.getDeclaredConstructors();

      // Checking if is necessary to make push-in for all declared constructors
      for (ConstructorMetadata constructor : allDeclaredConstructors) {
        // Check if current constructor exists on .java file
        classDetails = getTypeLocationService().getTypeDetails(detailsBuilder.build().getType());

        List<JavaType> parameterTypes = new ArrayList<JavaType>();
        for (AnnotatedJavaType type : constructor.getParameterTypes()) {
          parameterTypes.add(type.getJavaType());
        }

        ConstructorMetadata javaDeclaredConstructor =
            classDetails.getDeclaredConstructor(parameterTypes);

        // If not exists, add it!
        if (javaDeclaredConstructor == null) {
          // Add constructor to .java file
          detailsBuilder.addConstructor(constructor);
          String constructorParametersNames = "";
          for (JavaSymbolName paramName : constructor.getParameterNames()) {
            constructorParametersNames =
                constructorParametersNames.concat(paramName.getSymbolName()).concat(", ");
            changesToApply.append(String.format(
                "Constructor with parameters '%s' will be pushed on '%s.java' class. \n",
                constructorParametersNames.substring(0, constructorParametersNames.length() - 2),
                klass.getSimpleTypeName()));
          }
        }

      }

      // Getting all declared annotations (including declared on ITDs
      // and .java files)
      List<AnnotationMetadata> allDeclaredAnnotations = memberHoldingTypeDetails.getAnnotations();
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
          changesToApply.append(String.format(
              "Annotation '%s' will be pushed on '%s.java' class. \n", annotation
                  .getAnnotationType().getSimpleTypeName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all extends registered on .aj file to move to .java file
      List<JavaType> allExtendsTypes = memberHoldingTypeDetails.getExtendsTypes();
      for (JavaType extendsType : allExtendsTypes) {
        // If extends exists on .aj file, add it!
        if (!detailsBuilder.getExtendsTypes().contains(extendsType)) {
          detailsBuilder.addExtendsTypes(extendsType);
          changesToApply.append(String.format(
              "Extends type '%s' will be pushed on '%s.java' class. \n",
              extendsType.getSimpleTypeName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all implements registered on .aj file to move to .java file
      List<JavaType> allImplementsTypes = memberHoldingTypeDetails.getImplementsTypes();
      for (JavaType implementsType : allImplementsTypes) {
        if (!detailsBuilder.getImplementsTypes().contains(implementsType)) {
          detailsBuilder.addImplementsType(implementsType);
          changesToApply.append(String.format(
              "Implements type '%s' will be pushed on '%s.java' class. \n",
              implementsType.getSimpleTypeName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all imports registered on .aj file to move to .java file
      Set<ImportMetadata> allRegisteredImports = memberHoldingTypeDetails.getImports();
      detailsBuilder.addImports(allRegisteredImports);

    }

    // Updating .java file
    if (!force) {
      // Show message to be able to know which changes will be applied
      if (changesToApply.length() > 0) {
        LOGGER.log(Level.INFO, changesToApply.toString());
      }
    } else {
      getTypeManagementService().createOrUpdateTypeOnDisk(detailsBuilder.build());
    }
  }

  /**
   * Makes push-in of a method defined on a provided class
   * 
   * @param klass
   * @param method
   */
  public void pushInMethod(JavaType klass, MethodMetadata method) {
    // Check if current klass exists
    Validate
        .notNull(klass, "ERROR: You must specify a valid class to continue with push-in action");

    // Getting class details
    ClassOrInterfaceTypeDetails classDetails = getTypeLocationService().getTypeDetails(klass);
    Validate
        .notNull(klass, "ERROR: You must specify a valid class to continue with push-in action");

    Validate.notNull(method, "ERROR: You must provide a valid method");

    // Getting member details
    MemberDetails memberDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);

    // Check if the provided class is a test to be able to select valid class path
    Path path =
        classDetails.getAnnotation(RooJavaType.ROO_UNIT_TEST) == null ? Path.SRC_MAIN_JAVA
            : Path.SRC_TEST_JAVA;

    // Getting current class .java file metadata ID
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(klass,
            getPathResolver().getPath(klass.getModule(), path));

    // Getting detailsBuilder
    ClassOrInterfaceTypeDetailsBuilder detailsBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(classDetails);

    // Getting all details 
    for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {

      // Prevent that details from inheritance classes could be include on this .java file 
      if (!memberHoldingTypeDetails.getType().equals(classDetails.getType())) {
        continue;
      }

      // Getting all declared methods (including declared on ITDs
      // and .java files)
      List<MethodMetadata> allDeclaredMethods = memberHoldingTypeDetails.getMethods();

      // Checking if is necessary to make push-in for all declared methods
      for (MethodMetadata declaredMethod : allDeclaredMethods) {
        // If method exists on .aj file, add it!
        if (!method.getDeclaredByMetadataId().equals(declaredByMetadataId)
            && declaredMethod.equals(method)) {
          // Add method to .java file
          detailsBuilder.addMethod(getNewMethod(declaredByMetadataId, method));
        }
      }

      // Getting all imports registered on .aj file to move to .java file
      Set<ImportMetadata> allRegisteredImports = memberHoldingTypeDetails.getImports();
      detailsBuilder.addImports(allRegisteredImports);

    }

    // Updating .java file
    getTypeManagementService().createOrUpdateTypeOnDisk(detailsBuilder.build());

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
    methodBuilder.setAnnotations(method.getAnnotations());

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
   * This method checks if the provided methodName matches with 
   * the provided regular expression
   *  
   * @param methodName
   * @param regEx
   * @return
   */
  private boolean methodMatch(String methodName, String regEx) {
    // Create regular expression using provided text
    Pattern pattern = Pattern.compile(regEx);
    Matcher matcher = pattern.matcher(methodName);
    return matcher.matches();
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
