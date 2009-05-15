package org.springframework.roo.bootstrap;

import org.springframework.roo.process.manager.CommandCallback;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
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
@ScopeDevelopmentShell
public class ProcessManagerHostedExecutionStrategy implements ExecutionStrategy {

	private ProcessManager processManager;
	
	public ProcessManagerHostedExecutionStrategy(ProcessManager processManager) {
		Assert.notNull(processManager, "Process manager required");
		this.processManager = processManager;
	}

	public Object execute(final ParseResult parseResult) throws RuntimeException {
		Assert.notNull(parseResult, "Parse result required");
		return processManager.execute(new CommandCallback<Object>() {
			public Object callback() {
				return ReflectionUtils.invokeMethod(parseResult.getMethod(), parseResult.getInstance(), parseResult.getArguments());
			}
		});
	}

}
