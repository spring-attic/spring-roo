package org.springframework.roo.classpath;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Module feature that represents the modules which contain a class annotated with @SpringBootApplication
 * 
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class ApplicationModuleFeature implements ModuleFeature {

  // ------------ OSGi component attributes ----------------
  private BundleContext context;
  private TypeLocationService typeLocationService;
  private ProjectOperations projectOperations;

  protected final static Logger LOGGER = HandlerUtils.getLogger(ApplicationModuleFeature.class);

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public ModuleFeatureName getName() {
    return ModuleFeatureName.APPLICATION;
  }

  @Override
  public List<Pom> getModules() {
    List<Pom> modules = new ArrayList<Pom>();
    Pom module;

    for (String moduleName : getModuleNames()) {
      module = getProjectOperations().getPomFromModuleName(moduleName);
      if (module == null) {
        throw new NullPointerException(String.format("ERROR: Pom not found for module %s",
            moduleName));
      }
      modules.add(module);
    }
    return modules;
  }

  @Override
  public List<String> getModuleNames() {
    List<String> moduleNames = new ArrayList<String>();

    for (ClassOrInterfaceTypeDetails cid : getTypeLocationService()
        .findClassesOrInterfaceDetailsWithAnnotation(
            new JavaType("org.springframework.boot.autoconfigure.SpringBootApplication"))) {
      moduleNames.add(cid.getName().getModule());
    }
    return moduleNames;
  }

  @Override
  public boolean hasModuleFeature(Pom module) {
    return getModules().contains(module);
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
        LOGGER.warning("Cannot load TypeLocationService on ApplicationModuleFeature.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (ProjectOperations) context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on ApplicationModuleFeature.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }



}
