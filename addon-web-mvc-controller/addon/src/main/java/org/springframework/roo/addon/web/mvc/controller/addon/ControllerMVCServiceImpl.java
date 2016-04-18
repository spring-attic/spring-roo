package org.springframework.roo.addon.web.mvc.controller.addon;

import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ControllerMVCService}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ControllerMVCServiceImpl implements ControllerMVCService {

  private static final Logger LOGGER = HandlerUtils.getLogger(ControllerMVCServiceImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private TypeLocationService typeLocationService;
  private MemberDetailsScanner memberDetailsScanner;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public MethodMetadata getMVCMethodByRequestMapping(JavaType controller, String method,
      String path, List<String> params, String accept, String consumes, String produces,
      String headers) {

    // Getting controller member details
    MemberDetails controllerDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().toString(),
            getTypeLocationService().getTypeDetails(controller));

    // Getting all controller methods
    List<MethodMetadata> methods = controllerDetails.getMethods();

    for (MethodMetadata definedMethod : methods) {

      // Getting request mapping annotation
      AnnotationMetadata requesMappingAnnotation =
          definedMethod.getAnnotation(SpringJavaType.REQUEST_MAPPING);

      if (requesMappingAnnotation != null) {
        // Get all attributes
        String methodAttr =
            requesMappingAnnotation.getAttribute("method") != null ? (String) requesMappingAnnotation
                .getAttribute("method").getValue() : "";
        String valueAttr =
            requesMappingAnnotation.getAttribute("value") != null ? (String) requesMappingAnnotation
                .getAttribute("value").getValue() : "";

        // TODO: Get params and compare them

        String acceptAttr =
            requesMappingAnnotation.getAttribute("accept") != null ? (String) requesMappingAnnotation
                .getAttribute("accept").getValue() : "";
        String consumesAttr =
            requesMappingAnnotation.getAttribute("consumes") != null ? (String) requesMappingAnnotation
                .getAttribute("consumes").getValue() : "";
        String producesAttr =
            requesMappingAnnotation.getAttribute("produces") != null ? (String) requesMappingAnnotation
                .getAttribute("produces").getValue() : "";
        String headersAttr =
            requesMappingAnnotation.getAttribute("headers") != null ? (String) requesMappingAnnotation
                .getAttribute("headers").getValue() : "";

        // If every attribute match, return this method
        if (methodAttr.equals(method) && valueAttr.equals(path) && acceptAttr.equals(accept)
            && consumesAttr.equals(consumes) && producesAttr.equals(produces)
            && headersAttr.equals(headers)) {
          return definedMethod;
        }
      }
    }

    return null;
  }


  // Methods to obtain OSGi Services

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
        LOGGER.warning("Cannot load TypeLocationService on ControllerMVCServiceImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    if (memberDetailsScanner == null) {
      // Get all Services implement MemberDetailsScanner interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          memberDetailsScanner = (MemberDetailsScanner) this.context.getService(ref);
          return memberDetailsScanner;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MemberDetailsScanner on ControllerMVCServiceImpl.");
        return null;
      }
    } else {
      return memberDetailsScanner;
    }
  }
}
