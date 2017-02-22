package org.springframework.roo.addon.pushin;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
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
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

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
    serviceManager.activate(this.context);
  }

  private static final Logger LOGGER = HandlerUtils.getLogger(PushInOperationsImpl.class);

  private ServiceInstaceManager serviceManager = new ServiceInstaceManager();

  @Override
  public boolean isPushInCommandAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public List<Object> pushInAll(boolean writeOnDisk, boolean force) {

    List<Object> pushedElements = new ArrayList<Object>();
    List<JavaPackage> projectPackages = new ArrayList<JavaPackage>();

    // Getting all JavaTypes on current project
    for (String moduleName : getProjectOperations().getModuleNames()) {

      // ROO-3833: Push-in all following a specific order to avoid 
      // metadata dependencies errors
      List<JavaPackage> packagesForModule =
          getTypeLocationService().getPackagesForModule(
              getProjectOperations().getPomFromModuleName(moduleName));
      for (JavaPackage modulePackage : packagesForModule) {
        projectPackages.add(modulePackage);
      }

      Collection<JavaType> allDeclaredTypes =
          getTypeLocationService().getTypesForModule(
              getProjectOperations().getPomFromModuleName(moduleName));

      if (!force) {
        for (JavaType declaredType : allDeclaredTypes) {

          // Push-in all content from .aj files to .java files
          pushedElements.addAll(pushInClass(declaredType, writeOnDisk, force));
        }
      } else {
        for (JavaPackage modulePackage : packagesForModule) {
          pushIn(modulePackage, null, null, writeOnDisk);
          getFileManager().scan();
        }
      }
    }

    if (!force) {
      LOGGER
          .log(
              Level.INFO,
              "All these changes will be applied. Execute your previous push-in command using --force parameter to apply them.");
    }

    return pushedElements;
  }

  @Override
  public List<Object> pushIn(JavaPackage specifiedPackage, JavaType klass, String method,
      boolean writeOnDisk) {

    List<Object> pushedElements = new ArrayList<Object>();

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

      // If --method is provided, method should exist on the provided
      // class
      if (method != null) {

        boolean methodExists = false;
        MemberDetails classMemberDetails =
            getMemberDetailsScanner().getMemberDetails(getClass().getName(), classDetails);
        for (MethodMetadata classMethod : classMemberDetails.getMethods()) {
          if (methodMatch(classMethod, method)) {
            pushedElements.addAll(pushInMethod(klass, classMethod, writeOnDisk));
            methodExists = true;
          }
        }

        Validate.isTrue(methodExists, String.format(
            "ERROR: No methods found on class '%s' that matches with '%s' expression.",
            klass.getSimpleTypeName(), method));

      } else {
        // If method is not specified, push-in entire class elements
        pushedElements.addAll(pushInClass(klass, writeOnDisk, true));
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
            if (methodMatch(classMethod, method)) {
              pushedElements.addAll(pushInMethod(declaredType, classMethod, writeOnDisk));
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
          if (methodMatch(classMethod, method)) {
            pushedElements.addAll(pushInMethod(declaredType, classMethod, writeOnDisk));
            methodExists = true;
          }
        }

        // Scan and update files status
        getFileManager().scan();
      }

      Validate.isTrue(methodExists, String.format(
          "ERROR: No methods found on entire project that matches with '%s' expression.", method));

    } else if (specifiedPackage != null) {
      for (JavaType declaredType : allDeclaredTypes) {
        if (declaredType.getPackage().equals(specifiedPackage)) {
          pushedElements.addAll(pushInClass(declaredType, writeOnDisk, true));
        }
      }
    } else {
      LOGGER.log(Level.WARNING, "ERROR: You must specify at least one parameter. ");
    }

    return pushedElements;
  }

  /**
   * Makes push-in of all items defined on a provided class
   * 
   * @param klass
   *            class to make the push-in operation
   * @param weiteOnDisk
   *            indicates if pushed elements should be writed on .java file
   * @param force
   *            if some operation will produce several changes, this parameter
   *            should be true.
   * 
   * @return list of objects with all the pushed elements.
   */
  public List<Object> pushInClass(JavaType klass, boolean writeOnDisk, boolean force) {

    List<Object> pushedElements = new ArrayList<Object>();

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
    List<MemberHoldingTypeDetails> memberHoldingTypes = memberDetails.getDetails();

    // Return if the class has not associated ITD's
    if (memberHoldingTypes.size() == 1
        && memberHoldingTypes.get(0).getPhysicalTypeCategory() != PhysicalTypeCategory.ITD) {
      return pushedElements;
    }

    // Check if the provided class is a test to be able to select valid
    // class path
    Path path =
        classDetails.getAnnotation(RooJavaType.ROO_JPA_UNIT_TEST) == null ? Path.SRC_MAIN_JAVA
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

      // Prevent that details from inheritance classes could be include on
      // this .java file
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
          MethodMetadata newMethod = getNewMethod(declaredByMetadataId, method);
          detailsBuilder.addMethod(newMethod);
          // Save changes on pushed elements list
          pushedElements.add(newMethod);
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
          FieldMetadata newField = getNewField(declaredByMetadataId, field);
          detailsBuilder.addField(newField);
          // Save changes on pushed elements list
          pushedElements.add(newField);
          changesToApply.append(String.format("Field '%s' will be pushed on '%s.java' class. \n",
              field.getFieldName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all declared constructors (including declared on ITDs and
      // .java files)
      List<? extends ConstructorMetadata> allDeclaredConstructors =
          memberHoldingTypeDetails.getDeclaredConstructors();

      // Checking if is necessary to make push-in for all declared
      // constructors
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
          // Save changes on pushed elements list
          pushedElements.add(constructor);
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
          // Save changes on pushed elements list
          pushedElements.add(annotation);
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
          // Save changes on pushed elements list
          pushedElements.add(extendsType);
          changesToApply.append(String.format(
              "Extends type '%s' will be pushed on '%s.java' class. \n",
              extendsType.getSimpleTypeName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all implements registered on .aj file to move to .java
      // file
      List<JavaType> allImplementsTypes = memberHoldingTypeDetails.getImplementsTypes();
      for (JavaType implementsType : allImplementsTypes) {
        if (!detailsBuilder.getImplementsTypes().contains(implementsType)) {
          detailsBuilder.addImplementsType(implementsType);
          // Save changes on pushed elements list
          pushedElements.add(implementsType);
          changesToApply.append(String.format(
              "Implements type '%s' will be pushed on '%s.java' class. \n",
              implementsType.getSimpleTypeName(), klass.getSimpleTypeName()));
        }
      }

      // Getting all imports registered on .aj file to move to .java file
      Set<ImportMetadata> allRegisteredImports = memberHoldingTypeDetails.getImports();
      detailsBuilder.addImports(allRegisteredImports);
      // Save changes on pushed elements list
      pushedElements.add(allRegisteredImports);

    }

    // Updating .java file
    if (!force) {
      // Show message to be able to know which changes will be applied
      if (changesToApply.length() > 0) {
        LOGGER.log(Level.INFO, changesToApply.toString());
      }
    } else if (writeOnDisk) {
      getTypeManagementService().createOrUpdateTypeOnDisk(detailsBuilder.build());
    }

    return pushedElements;
  }

  /**
   * Makes push-in of a method defined on a provided class
   * 
   * @param klass
   *            class to make the push-in operation
   * @param weiteOnDisk
   *            indicates if pushed elements should be writed on .java file
   * 
   * @return list of objects with all the pushed elements.
   */
  public List<Object> pushInMethod(JavaType klass, MethodMetadata method, boolean writeOnDisk) {

    List<Object> pushedElements = new ArrayList<Object>();

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

    // Check if the provided class is a test to be able to select valid
    // class path
    Path path =
        classDetails.getAnnotation(RooJavaType.ROO_JPA_UNIT_TEST) == null ? Path.SRC_MAIN_JAVA
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

      // Prevent that details from inheritance classes could be include on
      // this .java file
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
          MethodMetadata newMethod = getNewMethod(declaredByMetadataId, method);
          detailsBuilder.addMethod(newMethod);
          // Save changes to be pushed
          pushedElements.add(newMethod);
        }
      }

      // Getting all imports registered on .aj file to move to .java file
      Set<ImportMetadata> allRegisteredImports = memberHoldingTypeDetails.getImports();
      detailsBuilder.addImports(allRegisteredImports);
      // Save imports to be pushed only if some method has been pushed
      if (!pushedElements.isEmpty()) {
        pushedElements.addAll(allRegisteredImports);
      }

    }

    // Updating .java file if write on disdk
    if (writeOnDisk) {
      getTypeManagementService().createOrUpdateTypeOnDisk(detailsBuilder.build());
    }

    return pushedElements;

  }

  /**
   * Method that obtains all declared fields and returns a list with 
   * the private ones.
   * 
   * @param memberDetails
   * @return list with the private fields
   */
  private List<? extends FieldMetadata> getPrivateFields(MemberDetails memberDetails) {
    List<FieldMetadata> privateFields = new ArrayList<FieldMetadata>();
    // Checking all registered fields in ITDs and .java files
    for (FieldMetadata field : memberDetails.getFields()) {
      if (field.getModifier() == Modifier.PRIVATE
          || field.getModifier() == Modifier.PRIVATE + Modifier.FINAL) {
        privateFields.add(field);
      }
    }
    return privateFields;
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
    // ROO-3834: Including default comment structure during push-in
    methodBuilder.setCommentStructure(method.getCommentStructure());

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
   * This method checks if the provided methodName matches with the provided
   * regular expression
   * 
   * @param methodName
   * @param regEx
   * @return
   */
  private boolean methodMatch(MethodMetadata method, String regEx) {

    // Create regular expression using provided text
    Pattern pattern = Pattern.compile(regEx);
    Matcher matcher = pattern.matcher(method.getMethodName().getSymbolName());
    boolean matches = matcher.matches();

    // If not matches, maybe is not a regEx, so is necessary to check it
    // manually
    if (!matches && regEx.split("\\(").length > 1) {

      // Getting method name and parameter types
      String name = regEx.split("\\(")[0];
      String[] parameterTypes = regEx.split("\\(")[1].replaceAll("\\)", "").split(",");

      // Prevent errors with empty regular expressions
      if (StringUtils.isEmpty(name)) {
        return false;
      }

      if (method.getMethodName().equals(new JavaSymbolName(name))) {
        List<AnnotatedJavaType> methodParams = method.getParameterTypes();
        boolean sameParameterTypes = false;
        if (methodParams.size() == parameterTypes.length) {
          sameParameterTypes = true;
          for (int i = 0; i < methodParams.size(); i++) {
            if (!methodParams.get(i).getJavaType().getSimpleTypeName().equals(parameterTypes[i])) {
              sameParameterTypes = false;
              break;
            }
          }
        }

        // If the same method as provided, return true
        if (sameParameterTypes) {
          return true;
        }
      }
    }

    return matches;
  }

  /**
   * Method to obtain ProjectOperation service implementation
   * 
   * @return
   */
  public ProjectOperations getProjectOperations() {
    return serviceManager.getServiceInstance(this, ProjectOperations.class);
  }

  /**
   * Method to obtain TypeLocationService service implementation
   * 
   * @return
   */
  public TypeLocationService getTypeLocationService() {
    return serviceManager.getServiceInstance(this, TypeLocationService.class);
  }

  /**
   * Method to obtain MemberDetailsScanner service implementation
   * 
   * @return
   */
  public MemberDetailsScanner getMemberDetailsScanner() {
    return serviceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  /**
   * Method to obtain TypeManagementService service implementation
   * 
   * @return
   */
  public TypeManagementService getTypeManagementService() {
    return serviceManager.getServiceInstance(this, TypeManagementService.class);
  }

  /**
   * Method to obtain PathResolver service implementation
   * 
   * @return
   */
  public PathResolver getPathResolver() {
    return serviceManager.getServiceInstance(this, PathResolver.class);
  }

  /**
   * Method to obtain FileManager service implementation
   * 
   * @return
   */
  public FileManager getFileManager() {
    return serviceManager.getServiceInstance(this, FileManager.class);
  }

}
