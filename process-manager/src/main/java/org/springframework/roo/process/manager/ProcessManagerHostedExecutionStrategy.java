package org.springframework.roo.process.manager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.ReflectionUtils;

/**
 * Used to dispatch shell {@link ExecutionStrategy} requests
 * through {@link ProcessManager#execute(org.springframework.roo.process.manager.CommandCallback)}.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public class ProcessManagerHostedExecutionStrategy implements ExecutionStrategy {

	@Reference private ProcessManager processManager;
	
	protected void activate(ComponentContext context) {
	}

	public Object execute(final ParseResult parseResult) throws RuntimeException {
		Assert.notNull(parseResult, "Parse result required");
		return processManager.execute(new CommandCallback<Object>() {
			public Object callback() {
				return ReflectionUtils.invokeMethod(parseResult.getMethod(), parseResult.getInstance(), parseResult.getArguments());
			}
		});
	}

	public boolean isReadyForCommands() {
		return !processManager.getProcessManagerStatus().equals(ProcessManagerStatus.STARTING) && !processManager.getProcessManagerStatus().equals(ProcessManagerStatus.BUSY_POLLING);
	}

	
}
