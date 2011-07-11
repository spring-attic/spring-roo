package org.springframework.roo.shell.jline.osgi;

import java.net.URL;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.Parser;
import org.springframework.roo.shell.jline.JLineShell;
import org.springframework.roo.support.osgi.UrlFindingUtils;

/**
 * OSGi component launcher for {@link JLineShell}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class JLineShellComponent extends JLineShell {
    @Reference private ExecutionStrategy executionStrategy;
    @Reference private Parser parser;
	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
		Thread thread = new Thread(this, "Spring Roo JLine Shell");
		thread.start();
	}

	protected void deactivate(ComponentContext context) {
		this.context = null;
		closeShell();
	}

	protected Set<URL> findUrls(String resourceName) {
		return UrlFindingUtils.findUrls(context.getBundleContext(), "/" + resourceName);
	}

	@Override
	protected ExecutionStrategy getExecutionStrategy() {
		return executionStrategy;
	}

	@Override
	protected Parser getParser() {
		return parser;
	}
}