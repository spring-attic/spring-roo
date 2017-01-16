package org.springframework.roo.addon.ws.addon;


import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.ws.annotations.RooSei;
import org.springframework.roo.addon.ws.annotations.RooSeiImpl;
import org.springframework.roo.addon.ws.annotations.RooWsClients;
import org.springframework.roo.addon.ws.annotations.RooWsEndpoints;
import org.springframework.roo.addon.ws.annotations.SoapBindingType;
import org.springframework.roo.application.config.ApplicationConfigService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Implementation of the {@link WsOperations}. It contains all the necessary
 * implementations for the defined operations in {@link WsOperations}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class WsOperationsImpl implements WsOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(WsOperationsImpl.class);

  private static final Property CXF_PROPERTY = new Property("cxf.version", "3.1.8");
  private static final Dependency CXF_RT_FRONTEND_JAXWS_DEPENDENCY = new Dependency(
      "org.apache.cxf", "cxf-rt-frontend-jaxws", "${cxf.version}");
  private static final Dependency CXF_RT_TRANSPORTS_HTTP_DEPENDENCY = new Dependency(
      "org.apache.cxf", "cxf-rt-transports-http", "${cxf.version}");
  private static final Dependency CXF_STARTER_DEPENDENCY = new Dependency("org.apache.cxf",
      "cxf-spring-boot-starter-jaxws", "${cxf.version}");

  private static final Property TRACEE_PROPERTY = new Property("tracee.version", "1.1.2");
  private static final Dependency TRACEE_JAXWS_DEPENDENCY = new Dependency("io.tracee.binding",
      "tracee-jaxws", "${tracee.version}");

  private static final Dependency TRACEE_CXF_DEPENDENCY = new Dependency("io.tracee.binding",
      "tracee-cxf", "${tracee.version}");


  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  @Override
  public boolean areWsCommandsAvailable() {
    return getProjectOperations().isFocusedProjectAvailable();
  }

  @Override
  public void addWsClient(String wsdlLocation, String endPoint, JavaType configClass,
      SoapBindingType bindingType, String serviceUrl, String profile) {

    Validate.notEmpty(wsdlLocation, "ERROR: Provide a valid wsdlLocation");
    Validate.notEmpty(endPoint, "ERROR: Provide a valid endPoint");
    Validate.notNull(configClass, "ERROR: Provide a valid configClass");

    // Check if the configClass is located in an application module
    if (!isLocatedInApplicationModule(configClass)) {
      LOGGER.log(Level.INFO,
          "ERROR: The provided config class is not located in an application module.");
      return;
    }

    // Getting module from the .wsdl file
    final Pom wsdlModule = getModuleFromWsdlLocation(wsdlLocation);
    final String wsdlModuleName = wsdlModule.getModuleName();
    // Getting the wsdlName without the module
    final String wsdlName = getWsdlNameWithoutModule(wsdlLocation);

    // Getting wsdl absolute path from the provided wsdlLocation
    final String wsdlPath = getWsdlAbsolutePathFromWsdlName(wsdlLocation);

    // Check if provided .wsdl exists
    Validate.isTrue(getFileManager().exists(wsdlPath),
        "ERROR: You must provide an existing .wsdl file.");

    // To prevent compilation errors, is necessary to include dependencies between 
    // the configClass module and the .wsdl module
    if (wsdlModuleName != configClass.getModule()) {
      getProjectOperations().addDependency(configClass.getModule(),
          new Dependency(wsdlModule.getGroupId(), wsdlModule.getArtifactId(), null));
    }

    // Check if provided configClass exists or should be generated
    boolean isNewConfigClass = false;
    ClassOrInterfaceTypeDetails configClassDetails =
        getTypeLocationService().getTypeDetails(configClass);
    if (configClassDetails == null) {
      isNewConfigClass = true;
    }

    // If exists, is necessary to check if is a configuration class and if it has some specific profile.
    // If it have it, it should match with the provided one the provided one
    if (!isNewConfigClass) {

      MemberDetails configClassMemberDetails =
          getMemberDetailsScanner().getMemberDetails(getClass().getName(), configClassDetails);
      AnnotationMetadata configurationAnnotation =
          configClassMemberDetails.getAnnotation(SpringJavaType.CONFIGURATION);
      if (configurationAnnotation == null) {
        LOGGER
            .log(
                Level.INFO,
                "ERROR: The provided class is not annotated with @Configuration so is not possible to include Web Service client configuration on it."
                    + "Specify other configuration class that contains @Configuration annotation or specify a not existing class to generate it.");
        return;
      }

      if (StringUtils.isNotEmpty(profile)) {
        AnnotationMetadata profileAnnotation =
            configClassMemberDetails.getAnnotation(SpringJavaType.PROFILE);
        if (profileAnnotation != null) {
          String profiles = (String) profileAnnotation.getAttribute("value").getValue();
          String[] definedProfiles = profiles.split(",");
          boolean profileExists = false;
          for (String definedProfile : definedProfiles) {
            if (definedProfile.equals(profile)) {
              profileExists = true;
            }
          }

          if (!profileExists) {
            LOGGER.log(Level.INFO,
                "ERROR: The provided configuration class doesn't work in the provided profile. "
                    + "Use a different configuration class or use a different profile.");
            return;
          }
        }
      }

    }

    // Obtain the service URL from the provided .wsdl file if empty
    if (StringUtils.isEmpty(serviceUrl)) {
      serviceUrl = getServiceUrlForEndpointFromWsdlFile(endPoint, wsdlPath);
      Validate
          .notEmpty(
              serviceUrl,
              "ERROR: It has not been possible to obtain the URL of the service from the provided .wsdl file. Indicate some serviceUrl using --serviceUrl parameter");
    }


    // Obtain the binding type from the provided .wsdl file if empty
    if (bindingType == null) {
      bindingType = getBindingTypeFromWsdlFile(wsdlPath);
      Validate
          .notNull(
              bindingType,
              "ERROR: It has not been possible to obtain the BindingType of the service from the provided .wsdl file. Indicate an specific BindingType using --binding parameter");
    }

    // Always is necessary to obtain the targetNameSpace from the provided .wsdl file 
    String targetNameSpace = getTargetNameSpaceFromWsdlFile(wsdlPath);
    Validate
        .notEmpty(
            targetNameSpace,
            "ERROR: It has not been possible to obtain the targetNamespace of the service from the provided .wsdl file. Check if your .wsdl file has the correct format.");

    // Include necessary dependencies and plugins
    includeDependenciesAndPluginsForWsClient(wsdlName, wsdlModuleName);

    // Include the necessary properties using the provided profile
    getApplicationConfigService().addProperty(configClass.getModule(), "url/".concat(endPoint),
        serviceUrl, profile, true);

    // Generating the new configuration class if not exists
    // If provided class already exists, update it
    ClassOrInterfaceTypeDetailsBuilder cidBuilder = null;
    if (!isNewConfigClass) {
      // Obtain builder from the existing class
      cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(configClassDetails);

      // Check if already have @RooWsClients annotation
      AnnotationMetadataBuilder wsClientsAnnotation =
          cidBuilder.getDeclaredTypeAnnotation(RooJavaType.ROO_WS_CLIENTS);
      if (wsClientsAnnotation != null) {
        // Update the existing one
        AnnotationAttributeValue<?> existingEndPoints =
            wsClientsAnnotation.build().getAttribute("endpoints");
        List<?> values = (List<?>) existingEndPoints.getValue();

        if (values != null) {

          // Check if the provided endpoint exists yet in this config class
          Iterator<?> it = values.iterator();
          boolean alreadyManaged = false;
          while (it.hasNext()) {
            NestedAnnotationAttributeValue existingEndPoint =
                (NestedAnnotationAttributeValue) it.next();
            String existingEndpointName =
                (String) existingEndPoint.getValue().getAttribute("endpoint").getValue();
            if (existingEndpointName.equals(endPoint)) {
              alreadyManaged = true;
            }
          }

          // If endpoint already exists, show an error indicating that this endpoint is already managed
          if (alreadyManaged) {
            LOGGER.log(Level.INFO,
                "ERROR: The provided endpoint is already defined in the provided configuration class. "
                    + "Specify some different configuration class.");
            return;
          } else {
            // Update existing annotation with the new endPoint
            Iterator<?> iterator = values.iterator();
            List<AnnotationAttributeValue<?>> endpoints =
                new ArrayList<AnnotationAttributeValue<?>>();
            while (iterator.hasNext()) {
              NestedAnnotationAttributeValue existingEndPoint =
                  (NestedAnnotationAttributeValue) iterator.next();
              String existingEndpointName =
                  (String) existingEndPoint.getValue().getAttribute("endpoint").getValue();
              String existingEndpointNameSpace =
                  (String) existingEndPoint.getValue().getAttribute("targetNamespace").getValue();
              EnumDetails existingType =
                  (EnumDetails) existingEndPoint.getValue().getAttribute("binding").getValue();

              // Create @RooWsClient annotation
              NestedAnnotationAttributeValue existingEndpoint =
                  new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
                      getWsClientAnnotation(existingEndpointName, existingEndpointNameSpace,
                          existingType).build());
              endpoints.add(existingEndpoint);
            }

            // Create @RooWsClient annotation
            NestedAnnotationAttributeValue newEndpoint =
                new NestedAnnotationAttributeValue(new JavaSymbolName("value"),
                    getWsClientAnnotation(endPoint, targetNameSpace, bindingType).build());
            endpoints.add(newEndpoint);

            ArrayAttributeValue<AnnotationAttributeValue<?>> newEndpoints =
                new ArrayAttributeValue<AnnotationAttributeValue<?>>(
                    new JavaSymbolName("endpoints"), endpoints);
            wsClientsAnnotation.addAttribute(newEndpoints);
          }

        }

      } else {
        // If not exists, add it with the new elements
        wsClientsAnnotation = new AnnotationMetadataBuilder(new JavaType(RooWsClients.class));

        // Create @RooWsClient annotation
        List<AnnotationAttributeValue<?>> endpoints = new ArrayList<AnnotationAttributeValue<?>>();

        NestedAnnotationAttributeValue newEndpoint =
            new NestedAnnotationAttributeValue(new JavaSymbolName("value"), getWsClientAnnotation(
                endPoint, targetNameSpace, bindingType).build());
        endpoints.add(newEndpoint);
        ArrayAttributeValue<AnnotationAttributeValue<?>> newEndpoints =
            new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("endpoints"),
                endpoints);

        wsClientsAnnotation.addAttribute(newEndpoints);
        if (StringUtils.isNotEmpty(profile)) {
          wsClientsAnnotation.addStringAttribute("profile", profile);
        }

        // Include new @RooWsClients annotation
        cidBuilder.addAnnotation(wsClientsAnnotation);
      }

    } else {
      // Create new configuration class
      final String configClassIdentifier =
          getPathResolver().getCanonicalPath(configClass.getModule(), Path.SRC_MAIN_JAVA,
              configClass);
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(configClass,
              getPathResolver().getPath(configClassIdentifier));
      cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(mid, Modifier.PUBLIC, configClass,
              PhysicalTypeCategory.CLASS);

      // Create new @RooWsClients annotation
      AnnotationMetadataBuilder wsClientsAnnotation =
          new AnnotationMetadataBuilder(new JavaType(RooWsClients.class));

      // Create @RooWsClient annotation
      List<AnnotationAttributeValue<?>> endpoints = new ArrayList<AnnotationAttributeValue<?>>();

      NestedAnnotationAttributeValue newEndpoint =
          new NestedAnnotationAttributeValue(new JavaSymbolName("value"), getWsClientAnnotation(
              endPoint, targetNameSpace, bindingType).build());
      endpoints.add(newEndpoint);
      ArrayAttributeValue<AnnotationAttributeValue<?>> newEndpoints =
          new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("endpoints"),
              endpoints);

      wsClientsAnnotation.addAttribute(newEndpoints);
      if (StringUtils.isNotEmpty(profile)) {
        wsClientsAnnotation.addStringAttribute("profile", profile);
      }

      // Include new @RooWsClients annotation
      cidBuilder.addAnnotation(wsClientsAnnotation);
    }

    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

    // Compile project to be able to generate necessary resources.
    // Is necessary to create new thread and wat for it.
    Thread generateSourcesThread = new Thread() {
      public void run() {
        try {
          Thread.sleep(1000);
          final StringBuilder sb = new StringBuilder();
          sb.append(LINE_SEPARATOR);
          sb.append(LINE_SEPARATOR);
          sb.append("##########################################################").append(
              LINE_SEPARATOR);
          sb.append("##########################################################").append(
              LINE_SEPARATOR);
          sb.append("################# Generating client sources ##############").append(
              LINE_SEPARATOR);
          sb.append("##########################################################").append(
              LINE_SEPARATOR);
          sb.append("##########################################################").append(
              LINE_SEPARATOR);
          sb.append("#").append(LINE_SEPARATOR);
          sb.append("# Please wait...").append(LINE_SEPARATOR);
          sb.append("# Don't execute any command until this operation finishes.").append(
              LINE_SEPARATOR);
          sb.append("#").append(LINE_SEPARATOR);
          sb.append(LINE_SEPARATOR);
          sb.append(LINE_SEPARATOR);
          LOGGER.log(Level.INFO, sb.toString());
          // Changing focus to the module where the .wsdl file is located
          getProjectOperations().setModule(wsdlModule);
          // executing mvn generate-sources command
          getMavenOperations().executeMvnCommand("generate-sources");
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };

    generateSourcesThread.start();
  }

  @Override
  public void addSEI(JavaType service, JavaType sei, JavaType endpointClass, JavaType configClass,
      String profile, boolean force) {

    Validate.notNull(service, "ERROR: Provide a valid service");
    Validate.notNull(sei, "ERROR: Provide a valid sei");

    // Check if provided service exists
    ClassOrInterfaceTypeDetails serviceTypeDetails =
        getTypeLocationService().getTypeDetails(service);
    Validate.notNull(serviceTypeDetails, "ERROR: Provide an existing service");

    // Check if provided service is annotated with @RooService
    AnnotationMetadata serviceAnnotation =
        serviceTypeDetails.getAnnotation(RooJavaType.ROO_SERVICE);
    Validate
        .notNull(serviceAnnotation, "ERROR: Provide a valid service annotated with @RooService");

    // Check if provided service has a related entity 
    AnnotationAttributeValue<JavaType> entityAttr = serviceAnnotation.getAttribute("entity");
    Validate.notNull(entityAttr,
        "ERROR: The provided service is annotated with @RooService but doesn't "
            + "contains the 'entity' attribute");
    JavaType relatedEntity = entityAttr.getValue();
    Validate.notNull(relatedEntity,
        "ERROR: The provided service is annotated with @RooService but doesn't "
            + "contains a valid entity in the 'entity' attribute");

    // Check if provided SEI is located in an application module
    if (!isLocatedInApplicationModule(sei) && !force) {
      LOGGER.log(Level.INFO, "ERROR: The provided SEI is not located in an application module.");
      return;
    }

    // Check if the configClass is located in an application module
    if (configClass != null && !isLocatedInApplicationModule(configClass) && !force) {
      LOGGER.log(Level.INFO,
          "ERROR: The provided config class is not located in an application module.");
      return;
    }

    // If developer has not specify any EndPoint class is necessary to generate 
    // new one inside the provided SEI module using the provided SEI name and 'Endpoint' suffix.
    if (endpointClass == null) {
      endpointClass =
          new JavaType(String.format("%sEndpoint", sei.getFullyQualifiedTypeName()),
              sei.getModule());
    }

    // If developer has not specify any configuration class is necessary to generate a
    // new one inside the provided SEI module using the provided SEI name and 'Configuration' suffix
    if (configClass == null) {
      configClass =
          new JavaType(String.format("%sConfiguration", sei.getFullyQualifiedTypeName()),
              sei.getModule());
    }

    // Check if provided configClass exists or should be generated
    boolean isNewConfigClass = false;
    ClassOrInterfaceTypeDetails configClassDetails =
        getTypeLocationService().getTypeDetails(configClass);
    if (configClassDetails == null) {
      isNewConfigClass = true;
    }

    // If exists, is necessary to check if is a configuration class and if it has some specific profile.
    // If it have it, it should match with the provided one the provided one
    if (!isNewConfigClass) {

      MemberDetails configClassMemberDetails =
          getMemberDetailsScanner().getMemberDetails(getClass().getName(), configClassDetails);
      AnnotationMetadata configurationAnnotation =
          configClassMemberDetails.getAnnotation(SpringJavaType.CONFIGURATION);
      if (configurationAnnotation == null) {
        LOGGER
            .log(
                Level.INFO,
                "ERROR: The provided class is not annotated with @Configuration so is not possible to include Web Service client configuration on it."
                    + "Specify other configuration class that contains @Configuration annotation or specify a not existing class to generate it.");
        return;
      }

      if (StringUtils.isNotEmpty(profile)) {
        AnnotationMetadata profileAnnotation =
            configClassMemberDetails.getAnnotation(SpringJavaType.PROFILE);
        if (profileAnnotation != null) {
          String profiles = (String) profileAnnotation.getAttribute("value").getValue();
          String[] definedProfiles = profiles.split(",");
          boolean profileExists = false;
          for (String definedProfile : definedProfiles) {
            if (definedProfile.equals(profile)) {
              profileExists = true;
            }
          }

          if (!profileExists) {
            LOGGER.log(Level.INFO,
                "ERROR: The provided configuration class doesn't work in the provided profile. "
                    + "Use a different configuration class or use a different profile.");
            return;
          }
        }
      }

    }


    // Check if some the provided classes that should be generated already exists
    if (getTypeLocationService().getTypeDetails(sei) != null) {
      LOGGER.log(Level.INFO,
          "ERROR: The provided SEI already exists. Specify a different one using"
              + " --sei parameter.");
      return;
    }

    if (getTypeLocationService().getTypeDetails(endpointClass) != null) {
      LOGGER.log(Level.INFO,
          "ERROR: The provided Endpoint class already exists. Specify a different one using"
              + " --class parameter.");
      return;
    }

    // Include necessary dependencies
    includeDependenciesAndPluginsForSei(sei.getModule());

    // Include the necessary properties using the provided profile
    getApplicationConfigService().addProperty(sei.getModule(), "cxf.path", "/services", profile,
        true);
    getApplicationConfigService().addProperty(sei.getModule(), "cxf.servlet.load-on-startup", "-1",
        profile, true);

    // Generate the new SEI
    final String seiIdentifier =
        getPathResolver().getCanonicalPath(sei.getModule(), Path.SRC_MAIN_JAVA, sei);
    final String midSEI =
        PhysicalTypeIdentifier.createIdentifier(sei, getPathResolver().getPath(seiIdentifier));
    ClassOrInterfaceTypeDetailsBuilder cidBuilderSEI =
        new ClassOrInterfaceTypeDetailsBuilder(midSEI, Modifier.PUBLIC, sei,
            PhysicalTypeCategory.INTERFACE);
    // Create new @RooWsEndpoint annotation
    AnnotationMetadataBuilder seiAnnotation =
        new AnnotationMetadataBuilder(new JavaType(RooSei.class));
    // Including service parameter to @RooSei annotation
    seiAnnotation.addClassAttribute("service", service);
    // Include new @RooSei annotation
    cidBuilderSEI.addAnnotation(seiAnnotation);

    // Write SEI class on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilderSEI.build());

    // Generate the new Endpoint
    final String endpointIdentifier =
        getPathResolver().getCanonicalPath(endpointClass.getModule(), Path.SRC_MAIN_JAVA,
            endpointClass);
    final String midEndpoint =
        PhysicalTypeIdentifier.createIdentifier(endpointClass,
            getPathResolver().getPath(endpointIdentifier));
    ClassOrInterfaceTypeDetailsBuilder cidBuilderEndpoint =
        new ClassOrInterfaceTypeDetailsBuilder(midEndpoint, Modifier.PUBLIC, endpointClass,
            PhysicalTypeCategory.CLASS);
    // Create new @RooSeiImpl annotation
    AnnotationMetadataBuilder endpointAnnotation =
        new AnnotationMetadataBuilder(new JavaType(RooSeiImpl.class));
    // Include sei parameter to @RooSeiImpl annotation
    endpointAnnotation.addClassAttribute("sei", sei);
    // Include new @RooSeiImpl annotation
    cidBuilderEndpoint.addAnnotation(endpointAnnotation);
    // Include implements
    cidBuilderEndpoint.addImplementsType(sei);

    // Write endpoint class on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilderEndpoint.build());

    // If configuration class exists, check if is already annotated and update it.
    // If not exists, create a new one
    ClassOrInterfaceTypeDetailsBuilder cidBuilderConfig = null;
    if (!isNewConfigClass) {
      // Obtain builder from the existing class
      cidBuilderConfig = new ClassOrInterfaceTypeDetailsBuilder(configClassDetails);

      // Check if already have @RooWsEndpoints annotation
      AnnotationMetadataBuilder wsEndpointsAnnotation =
          cidBuilderConfig.getDeclaredTypeAnnotation(RooJavaType.ROO_WS_ENDPOINTS);
      if (wsEndpointsAnnotation != null) {

        // Update the existing one
        AnnotationAttributeValue<?> existingEndPoints =
            wsEndpointsAnnotation.build().getAttribute("endpoints");
        List<?> values = (List<?>) existingEndPoints.getValue();

        if (values != null) {

          // Check if the provided endpoint exists yet in this config class
          Iterator<?> it = values.iterator();
          boolean alreadyManaged = false;
          while (it.hasNext()) {
            ClassAttributeValue existingEndPointAttr = (ClassAttributeValue) it.next();
            JavaType existingEndPoint = existingEndPointAttr.getValue();
            if (existingEndPoint.getFullyQualifiedTypeName().equals(
                endpointClass.getFullyQualifiedTypeName())) {
              alreadyManaged = true;
            }
          }

          // If endpoint already exists, show an error indicating that this endpoint is already managed
          if (alreadyManaged) {
            LOGGER.log(Level.INFO,
                "ERROR: The provided endpoint is already defined in the provided configuration class. "
                    + "Specify some different configuration class.");
            return;
          } else {
            // Update existing annotation with the new endPoint
            Iterator<?> iterator = values.iterator();
            List<AnnotationAttributeValue<?>> endpoints =
                new ArrayList<AnnotationAttributeValue<?>>();
            while (iterator.hasNext()) {
              ClassAttributeValue existingEndPoint = (ClassAttributeValue) iterator.next();
              endpoints.add(existingEndPoint);
            }

            // Create @RooWsEndpoints annotation
            ClassAttributeValue newEndpoint =
                new ClassAttributeValue(new JavaSymbolName("value"), endpointClass);
            endpoints.add(newEndpoint);

            ArrayAttributeValue<AnnotationAttributeValue<?>> newEndpoints =
                new ArrayAttributeValue<AnnotationAttributeValue<?>>(
                    new JavaSymbolName("endpoints"), endpoints);
            wsEndpointsAnnotation.addAttribute(newEndpoints);
          }

        }


      } else {
        // If not exists, add it with the new elements
        wsEndpointsAnnotation = new AnnotationMetadataBuilder(new JavaType(RooWsEndpoints.class));

        // Generate new list of endpoints
        List<AnnotationAttributeValue<?>> endpoints = new ArrayList<AnnotationAttributeValue<?>>();
        ClassAttributeValue newEndpoint =
            new ClassAttributeValue(new JavaSymbolName("value"), endpointClass);
        endpoints.add(newEndpoint);
        ArrayAttributeValue<AnnotationAttributeValue<?>> newEndpoints =
            new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("endpoints"),
                endpoints);
        wsEndpointsAnnotation.addAttribute(newEndpoints);

        // Check if is necessary to include profile attribute
        if (StringUtils.isNotEmpty(profile)) {
          wsEndpointsAnnotation.addStringAttribute("profile", profile);
        }

        // Include new @RooWsEndpoints annotation
        cidBuilderConfig.addAnnotation(wsEndpointsAnnotation);
      }


    } else {
      // Create the specified configuration class and annotate it with necessary information
      final String configClassIdentifier =
          getPathResolver().getCanonicalPath(configClass.getModule(), Path.SRC_MAIN_JAVA,
              configClass);
      final String mid =
          PhysicalTypeIdentifier.createIdentifier(configClass,
              getPathResolver().getPath(configClassIdentifier));
      cidBuilderConfig =
          new ClassOrInterfaceTypeDetailsBuilder(mid, Modifier.PUBLIC, configClass,
              PhysicalTypeCategory.CLASS);

      // Create new @RooWsEndpoints annotation and include the new endpoint 
      // as endpoints attribute
      List<AnnotationAttributeValue<?>> endpoints = new ArrayList<AnnotationAttributeValue<?>>();
      ClassAttributeValue endPointAttributeValue =
          new ClassAttributeValue(new JavaSymbolName("value"), endpointClass);
      endpoints.add(endPointAttributeValue);
      ArrayAttributeValue<AnnotationAttributeValue<?>> newEndpoints =
          new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("endpoints"),
              endpoints);
      AnnotationMetadataBuilder wsEndpointsAnnotation =
          new AnnotationMetadataBuilder(new JavaType(RooWsEndpoints.class));
      wsEndpointsAnnotation.addAttribute(newEndpoints);

      // Include new @RooWsEndpoints annotation
      cidBuilderConfig.addAnnotation(wsEndpointsAnnotation);

      // Include @Profile annotation with the provided profile. This annotation
      // doesn't exists yet, because we're generating a new @Configuration class
      if (StringUtils.isNotEmpty(profile)) {
        wsEndpointsAnnotation.addStringAttribute("profile", profile);
      }

    }

    // Write config class on disk
    getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilderConfig.build());

    // After create the SEI and the Endpoint, is necessary to annotate related entity with
    // some JAX-B annotations if has not been annotated before

    /*ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(relatedEntity);
    if (entityDetails != null) {
      // Annotate the entity with @RooJaxbEntity. If this entity has a super class or that 
      // super class has another super class, etc. is necessary to annotate it too.
      annotateClassIfNeeded(entityDetails);
      // Also, is necessary to annotate @OneToMany, @ManyToOne and @ManyToMany fields detected in 
      // this class and in the super classes.
      annotateRelatedFieldsIfNeeded(entityDetails);
    }*/

    // Provisional changes to annotate all entities
    Set<ClassOrInterfaceTypeDetails> allEntities =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY);
    for (ClassOrInterfaceTypeDetails entity : allEntities) {
      // Annotate the entity with @RooJaxbEntity. If this entity has a super class or that 
      // super class has another super class, etc. is necessary to annotate it too.
      annotateClassIfNeeded(entity);
      // Also, is necessary to annotate @OneToMany, @ManyToOne and @ManyToMany fields detected in 
      // this class and in the super classes.
      annotateRelatedFieldsIfNeeded(entity);
    }

  }

  /**
   * This method annotates the provided class with @RooJaxbEntity. If this class extends
   * other classes, and that classes annotates other classes, etc. 
   * this method will annotate them.
   * 
   * @param entityDetails
   */
  private void annotateClassIfNeeded(ClassOrInterfaceTypeDetails entityDetails) {
    List<JavaType> extendsTypes = entityDetails.getExtendsTypes();
    for (JavaType extendsType : extendsTypes) {
      ClassOrInterfaceTypeDetails extendsTypeDetails =
          getTypeLocationService().getTypeDetails(extendsType);
      if (extendsTypeDetails != null
          && extendsTypeDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {

        // If annotation has not been included before, add it.
        if (extendsTypeDetails.getAnnotation(RooJavaType.ROO_JAXB_ENTITY) == null) {
          ClassOrInterfaceTypeDetailsBuilder cidBuilder =
              new ClassOrInterfaceTypeDetailsBuilder(extendsTypeDetails);
          // Include @RooJaxbEntity annotation
          AnnotationMetadataBuilder jaxbEntityAnnotation =
              new AnnotationMetadataBuilder(RooJavaType.ROO_JAXB_ENTITY);
          cidBuilder.addAnnotation(jaxbEntityAnnotation);

          // Write entity class on disk
          getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
        }


        // Repeat the same process until all super classes 
        // have been annotated
        if (!extendsTypeDetails.getExtendsTypes().isEmpty()) {
          annotateClassIfNeeded(extendsTypeDetails);
        }
      }
    }

    // If annotation has not been included before, add it.
    if (entityDetails.getAnnotation(RooJavaType.ROO_JAXB_ENTITY) == null) {
      ClassOrInterfaceTypeDetailsBuilder cidBuilder =
          new ClassOrInterfaceTypeDetailsBuilder(entityDetails);
      // Include @RooJaxbEntity annotation
      AnnotationMetadataBuilder jaxbEntityAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_JAXB_ENTITY);
      cidBuilder.addAnnotation(jaxbEntityAnnotation);

      // Write entity class on disk
      getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

  }

  /**
   * This method annotates the provided class with @RooJaxbEntity. If this class extends
   * other classes, and that classes annotates other classes, etc. 
   * this method will annotate them.
   * 
   * @param entityDetails
   */
  private void annotateRelatedFieldsIfNeeded(ClassOrInterfaceTypeDetails entityDetails) {
    // Getting details of the provided entity
    MemberDetails memberDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(), entityDetails);
    // Getting all its fields
    for (FieldMetadata entityField : memberDetails.getFields()) {
      // If is a relation field, should be annotated
      if (entityField.getAnnotation(JpaJavaType.ONE_TO_ONE) != null
          || entityField.getAnnotation(JpaJavaType.ONE_TO_MANY) != null
          || entityField.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
          || entityField.getAnnotation(JpaJavaType.MANY_TO_MANY) != null) {


        // Getting details of the annotated field
        JavaType fieldType = entityField.getFieldType();

        if (fieldType.isCommonCollectionType()) {
          fieldType = fieldType.getBaseType();
        }

        ClassOrInterfaceTypeDetails fieldDetails =
            getTypeLocationService().getTypeDetails(fieldType);

        // If is a valid entity
        if (fieldDetails != null && fieldDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
          // Delegates in annotateClassIfNeeded to annotate the related class field
          annotateClassIfNeeded(fieldDetails);
        }
      }
    }
  }

  @Override
  public Pom getModuleFromWsdlLocation(String wsdlLocation) {
    // Separate the wsdlLocation and the module
    String[] wsdlLocationParts = wsdlLocation.split(":");
    if (wsdlLocationParts.length > 1) {
      return getProjectOperations().getPomFromModuleName(wsdlLocationParts[0]);
    } else {
      return getProjectOperations().getPomFromModuleName("");
    }
  }

  @Override
  public String getWsdlNameWithoutModule(String wsdlLocation) {
    // Separate the wsdlLocation and the module
    String[] wsdlLocationParts = wsdlLocation.split(":");
    if (wsdlLocationParts.length > 1) {
      return wsdlLocationParts[1];
    } else {
      return wsdlLocation;
    }
  }

  @Override
  public String getWsdlAbsolutePathFromWsdlName(String wsdlLocation) {
    // Getting root path
    String rootPath = getPathResolver().getRoot();

    // Getting module name and wsdl name from the provided wsdlName
    String wsdlName = getWsdlNameWithoutModule(wsdlLocation);
    String moduleName = getModuleFromWsdlLocation(wsdlLocation).getModuleName();

    return rootPath.concat("/").concat(moduleName).concat("/src/main/resources/").concat(wsdlName);
  }

  @Override
  public List<String> getEndPointsFromWsdlFile(String wsdlPath) {
    List<String> availableEndPoints = new ArrayList<String>();
    // Check if provided wsdl file exists
    if (getFileManager().exists(wsdlPath)) {
      // Obtain document
      final Document document = XmlUtils.readXml(getFileManager().getInputStream(wsdlPath));

      // Finding service elements
      List<Element> services = XmlUtils.findElements("service", document.getDocumentElement());
      for (Element service : services) {
        NodeList ports = service.getChildNodes();
        for (int i = 0; i < ports.getLength(); i++) {
          Element port = (Element) ports.item(1);
          if (port != null && port.getAttribute("name") != null) {
            availableEndPoints.add(port.getAttribute("name"));
          }
        }
      }
    }
    return availableEndPoints;
  }

  @Override
  public String getTargetNameSpaceFromWsdlFile(String wsdlPath) {
    // Check if provided wsdl file exists
    if (getFileManager().exists(wsdlPath)) {
      // Obtain document
      final Document document = XmlUtils.readXml(getFileManager().getInputStream(wsdlPath));
      // Return targetNamespace
      return document.getDocumentElement().getAttribute("targetNamespace");
    }

    return null;
  }

  @Override
  public SoapBindingType getBindingTypeFromWsdlFile(String wsdlPath) {
    // Check if provided wsdl file exists
    if (getFileManager().exists(wsdlPath)) {
      // Obtain document
      final Document document = XmlUtils.readXml(getFileManager().getInputStream(wsdlPath));
      // Get soap attribute
      String soapAttr = document.getDocumentElement().getAttribute("xmlns:soap");
      if ("http://schemas.xmlsoap.org/wsdl/soap/".equals(soapAttr)) {
        return SoapBindingType.SOAP11;
      } else if ("http://schemas.xmlsoap.org/wsdl/soap12/".equals(soapAttr)) {
        return SoapBindingType.SOAP12;
      } else if (soapAttr == null || "".equals(soapAttr)) {
        // Maybe attribute is called soap12
        String soap12Attr = document.getDocumentElement().getAttribute("xmlns:soap12");
        if ("http://schemas.xmlsoap.org/wsdl/soap12/".equals(soap12Attr)) {
          return SoapBindingType.SOAP12;
        }
      }
    }

    return null;
  }

  @Override
  public String getServiceUrlForEndpointFromWsdlFile(String endPoint, String wsdlPath) {
    // Check if provided wsdl file exists
    if (getFileManager().exists(wsdlPath)) {
      // Obtain document
      final Document document = XmlUtils.readXml(getFileManager().getInputStream(wsdlPath));

      // Finding service elements
      List<Element> services = XmlUtils.findElements("service", document.getDocumentElement());
      for (Element service : services) {
        NodeList ports = service.getChildNodes();
        for (int i = 0; i < ports.getLength(); i++) {
          Element port = (Element) ports.item(1);
          // Check if current endPoint has the same name as the provided one
          if (port != null && port.getAttribute("name") != null
              && endPoint.equals(port.getAttribute("name"))) {
            NodeList addresses = port.getChildNodes();
            for (int x = 0; x < addresses.getLength(); x++) {
              if (addresses.item(x) != null && addresses.item(x) instanceof Element) {
                Element address = (Element) addresses.item(x);
                return address.getAttribute("location");
              }
            }
          }
        }
      }
    }

    return null;
  }

  /**
   * This method provides @RooWsClient annotation with all the necessary attributes
   * 
   * @param endpoint
   * @param targetNamespace
   * @param bindingType
   * @return
   */
  private AnnotationMetadataBuilder getWsClientAnnotation(final String endpoint,
      final String targetNamespace, final SoapBindingType bindingType) {
    final List<AnnotationAttributeValue<?>> wsClientAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    wsClientAttributes.add(new StringAttributeValue(new JavaSymbolName("endpoint"), endpoint));
    wsClientAttributes.add(new StringAttributeValue(new JavaSymbolName("targetNamespace"),
        targetNamespace));
    wsClientAttributes.add(new EnumAttributeValue(new JavaSymbolName("binding"), new EnumDetails(
        RooJavaType.ROO_ENUM_SOAP_BINDING_TYPE, new JavaSymbolName(bindingType.name()))));
    return new AnnotationMetadataBuilder(RooJavaType.ROO_WS_CLIENT, wsClientAttributes);
  }

  /**
   * This method provides @RooWsClient annotation with all the necessary attributes
   * 
   * @param endpoint
   * @param targetNamespace
   * @param bindingType
   * @return
   */
  private AnnotationMetadataBuilder getWsClientAnnotation(final String endpoint,
      final String targetNamespace, final EnumDetails bindingType) {
    final List<AnnotationAttributeValue<?>> wsClientAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    wsClientAttributes.add(new StringAttributeValue(new JavaSymbolName("endpoint"), endpoint));
    wsClientAttributes.add(new StringAttributeValue(new JavaSymbolName("targetNamespace"),
        targetNamespace));
    wsClientAttributes.add(new EnumAttributeValue(new JavaSymbolName("binding"), bindingType));
    return new AnnotationMetadataBuilder(RooJavaType.ROO_WS_CLIENT, wsClientAttributes);
  }

  /**
   * This method includes the TracEE and CXF dependencies and includes the CXF
   * CodeGen Plugin with the necessary configuration for the new WsClient.
   * 
   * @param wsdlName
   *            the wsdl name
   * @param wsdlModuleName
   *            the module where the wsdl is located
   */
  private void includeDependenciesAndPluginsForWsClient(String wsdlName, String wsdlModuleName) {
    // Include CXF property if not exists
    getProjectOperations().addProperty("", CXF_PROPERTY);
    getProjectOperations().addDependency(wsdlModuleName, CXF_RT_FRONTEND_JAXWS_DEPENDENCY);
    getProjectOperations().addDependency(wsdlModuleName, CXF_RT_TRANSPORTS_HTTP_DEPENDENCY);

    // Include TracEE dependencies if not exists
    getProjectOperations().addProperty("", TRACEE_PROPERTY);
    getProjectOperations().addDependency(wsdlModuleName, TRACEE_CXF_DEPENDENCY);


    // Include CXF plugin if not exists
    final Element configuration = XmlUtils.getConfiguration(getClass());
    final List<Element> plugins =
        XmlUtils.findElements("/configuration/plugins/plugin", configuration);

    Plugin cxfPlugin = null;
    for (final Element pluginElement : plugins) {
      cxfPlugin = new Plugin(pluginElement);
      getProjectOperations().addBuildPlugin(wsdlModuleName, cxfPlugin);
      break;
    }

    // Update cxf plugin with the new wsdl
    Map<String, String> wsdlLocationProperties = new HashMap<String, String>();
    wsdlLocationProperties.put("wsdl", "${project.basedir}/src/main/resources/" + wsdlName);
    wsdlLocationProperties.put("wsdlLocation", "classpath:" + wsdlName);
    getProjectOperations().addElementToPluginExecution(wsdlModuleName, cxfPlugin,
        "generate-sources", "wsdlOptions", "wsdlOption", wsdlLocationProperties);
  }

  /**
   * This method includes the TracEE and CXF dependencies. Also, include the CXF starter
   * 
   * @param moduleName
   *            the module where the dependencies will be included
   */
  private void includeDependenciesAndPluginsForSei(String moduleName) {
    // Include CXF property if not exists
    getProjectOperations().addProperty("", CXF_PROPERTY);
    getProjectOperations().addDependency(moduleName, CXF_STARTER_DEPENDENCY);

    // Include TracEE dependencies if not exists
    getProjectOperations().addProperty("", TRACEE_PROPERTY);
    getProjectOperations().addDependency(moduleName, TRACEE_JAXWS_DEPENDENCY);
    getProjectOperations().addDependency(moduleName, TRACEE_CXF_DEPENDENCY);

  }

  /**
   * This method check if the provided element is located in an application
   * module or not
   * 
   * @param type
   */
  private boolean isLocatedInApplicationModule(JavaType type) {

    if (type == null) {
      return false;
    } else if ("".equals(type.getModule())) {
      return true;
    } else {

      String moduleName = type.getModule();

      // Getting all application modules
      Collection<Pom> modules = getTypeLocationService().getModules(ModuleFeatureName.APPLICATION);
      for (Pom pom : modules) {
        if (pom.getModuleName().equals(moduleName)) {
          return true;
        }
      }
    }

    return false;

  }

  // Obtaining OSGi services using ServiceInstaceManager utility

  public ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  public TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  public ApplicationConfigService getApplicationConfigService() {
    return serviceInstaceManager.getServiceInstance(this, ApplicationConfigService.class);
  }

  public TypeManagementService getTypeManagementService() {
    return serviceInstaceManager.getServiceInstance(this, TypeManagementService.class);
  }

  public PathResolver getPathResolver() {
    return serviceInstaceManager.getServiceInstance(this, PathResolver.class);
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  public FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  public MavenOperations getMavenOperations() {
    return serviceInstaceManager.getServiceInstance(this, MavenOperations.class);
  }

}
