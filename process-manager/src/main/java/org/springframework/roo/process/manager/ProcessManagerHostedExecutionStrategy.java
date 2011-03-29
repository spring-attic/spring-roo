package org.springframework.roo.process.manager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
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
@Reference(name="processManager", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=ProcessManager.class, cardinality=ReferenceCardinality.MANDATORY_UNARY)
public class ProcessManagerHostedExecutionStrategy implements ExecutionStrategy {

	private Class<?> mutex = ProcessManagerHostedExecutionStrategy.class;
	private ProcessManager processManager;

	protected void bindProcessManager(ProcessManager processManager) {
		synchronized (mutex) {
			this.processManager = processManager;
		}
	}
	
	protected void unbindProcessManager(ProcessManager processManager) {
		synchronized (mutex) {
			this.processManager = null;
		}
	}

	public Object execute(final ParseResult parseResult) throws RuntimeException {
		Assert.notNull(parseResult, "Parse result required");
		synchronized (mutex) {
			Assert.isTrue(isReadyForCommands(), "ProcessManagerHostedExecutionStrategy not yet ready for commands");
			return processManager.execute(new CommandCallback<Object>() {
				public Object callback() {
					return ReflectionUtils.invokeMethod(parseResult.getMethod(), parseResult.getInstance(), parseResult.getArguments());
				}
			});
		}
	}

	public boolean isReadyForCommands() {
		synchronized (mutex) {
			if (processManager != null) {
				// BUSY_EXECUTION needed in case of recursive commands, such as if executing a script
				// TERMINATED added in case of additional commands following a quit or exit in a script - ROO-2270
				return processManager.getProcessManagerStatus() == ProcessManagerStatus.AVAILABLE || processManager.getProcessManagerStatus() == ProcessManagerStatus.BUSY_EXECUTING || processManager.getProcessManagerStatus() == ProcessManagerStatus.TERMINATED;
			}
		}
		return false;
	}

	public void terminate() {
		synchronized (mutex) {
			if (processManager != null) {
				processManager.terminate();
			}
		}
	}

}
