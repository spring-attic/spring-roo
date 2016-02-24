package org.springframework.roo.shell.jline.osgi;

import static org.springframework.roo.support.util.AnsiEscapeCode.FG_CYAN;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_GREEN;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_MAGENTA;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_YELLOW;
import static org.springframework.roo.support.util.AnsiEscapeCode.REVERSE;
import static org.springframework.roo.support.util.AnsiEscapeCode.UNDERSCORE;
import static org.springframework.roo.support.util.AnsiEscapeCode.decorate;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.Parser;
import org.springframework.roo.shell.jline.JLineShell;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * OSGi component launcher for {@link JLineShell}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class JLineShellComponent extends JLineShell {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JLineShellComponent.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  @Reference
  ExecutionStrategy executionStrategy;
  @Reference
  Parser parser;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    final Thread thread = new Thread(this, "Spring Roo JLine Shell");
    thread.start();
  }

  protected void deactivate(final ComponentContext context) {
    this.context = null;
    closeShell();
  }

  @Override
  protected Collection<URL> findResources(final String path) {
    // For an OSGi bundle search, we add the root prefix to the given path
    return OSGiUtils.findEntriesByPath(context, OSGiUtils.ROOT_PATH + path);
  }

  @Override
  protected ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }


  @Override
  protected Parser getParser() {
    return parser;
  }

  @Override
  public String getStartupNotifications() {
    return null;
  }

}
