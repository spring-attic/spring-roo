package org.springframework.roo.addon.suite;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.subsystem.Subsystem;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * {@link Converter} for {@link AddonSuiteSymbolicName}.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class AddonSuiteSymbolicNameConverter implements Converter<AddonSuiteSymbolicName> {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils
      .getLogger(AddonSuiteSymbolicNameConverter.class);

  private List<Subsystem> installedSubsystems;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    installedSubsystems = new ArrayList<Subsystem>();
  }

  public AddonSuiteSymbolicName convertFromText(final String value, final Class<?> requiredType,
      final String optionContext) {
    return new AddonSuiteSymbolicName(value.trim());
  }

  public boolean getAllPossibleValues(final List<Completion> completions,
      final Class<?> requiredType, final String originalUserInput, final String optionContext,
      final MethodTarget target) {

    // Getting installed Roo Addon Suites
    populateRooAddonSuites();

    for (Subsystem subsystem : installedSubsystems) {
      completions.add(new Completion(subsystem.getSymbolicName()));
    }
    return false;
  }

  public boolean supports(final Class<?> requiredType, final String optionContext) {
    return AddonSuiteSymbolicName.class.isAssignableFrom(requiredType);
  }

  /**
   * Method to populate current Roo Addon Suites using OSGi Serive
   */
  private void populateRooAddonSuites() {

    installedSubsystems.clear();

    // Get all Services implement Subsystem interface
    try {
      ServiceReference<?>[] references =
          context.getAllServiceReferences(Subsystem.class.getName(), null);
      for (ServiceReference<?> ref : references) {
        Subsystem subsystem = (Subsystem) context.getService(ref);
        installedSubsystems.add(subsystem);
      }

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load Subsystem on AddonSymbolicName.");
    }
  }
}
