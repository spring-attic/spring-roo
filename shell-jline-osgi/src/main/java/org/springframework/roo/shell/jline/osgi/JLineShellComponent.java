package org.springframework.roo.shell.jline.osgi;

import java.net.URI;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.Parser;
import org.springframework.roo.shell.jline.JLineShell;
import org.springframework.roo.support.osgi.OSGiUtils;

/**
 * OSGi component launcher for {@link JLineShell}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class JLineShellComponent extends JLineShell {
	
	// Fields
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
	
	@Override
	protected Collection<URI> findResources(final String path) {
		// For an OSGi bundle search, we add the root prefix to the given path
		return OSGiUtils.findEntriesByPath(context.getBundleContext(), OSGiUtils.ROOT_PATH + path);
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