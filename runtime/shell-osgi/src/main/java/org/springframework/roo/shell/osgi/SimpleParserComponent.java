package org.springframework.roo.shell.osgi;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Parser;
import org.springframework.roo.shell.SimpleParser;
import org.springframework.roo.support.api.AddOnSearch;
import org.springframework.roo.support.api.AddOnSearch.SearchType;

/**
 * OSGi component launcher for {@link SimpleParser}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service(value = Parser.class)
@References(value = {@Reference(name = "addOnSearch", strategy = ReferenceStrategy.EVENT,
    policy = ReferencePolicy.DYNAMIC, referenceInterface = AddOnSearch.class,
    cardinality = ReferenceCardinality.OPTIONAL_UNARY)})
public class SimpleParserComponent extends SimpleParser implements CommandMarker {
  private AddOnSearch addOnSearch;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
  }

  protected void bindAddOnSearch(final AddOnSearch s) {
    addOnSearch = s;
  }

  @Override
  protected void commandNotFound(final Logger logger, final String buffer) {
    logger.warning("Command '" + buffer + "' not found (for assistance press "
        + AbstractShell.completionKeys + " or type \"hint\" then hit ENTER)");

    if (addOnSearch == null) {
      return;
    }

    // Decide which command they asked for
    String command = buffer.trim();

    // Truncate from the first option, if any was given
    final int firstDash = buffer.indexOf("--");
    if (firstDash > 1) {
      command = buffer.substring(0, firstDash - 1).trim();
    }

    // Do a silent (console message free) lookup of matches
    Integer matches = null;
    matches = addOnSearch.searchAddOns(command, SearchType.ADDON);

    // Render to screen if required
    if (matches == null) {
      logger.info("Spring Roo automatic add-on discovery service currently unavailable");
    }
  }

  protected void unbindAddOnSearch(final AddOnSearch s) {
    addOnSearch = null;
  }
}
