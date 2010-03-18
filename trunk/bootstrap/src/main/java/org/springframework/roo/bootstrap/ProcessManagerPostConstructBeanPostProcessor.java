package org.springframework.roo.bootstrap;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Invokes {@link ProcessManager#completeStartup()} once the context has started.
 * 
 * <p>
 * This class exists in the bootstrap module to avoid the process manager module needing to depend
 * on Spring Framework types.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class ProcessManagerPostConstructBeanPostProcessor implements ApplicationListener<ContextRefreshedEvent> {

	private ProcessManager processManager;
	
	public ProcessManagerPostConstructBeanPostProcessor(ProcessManager processManager) {
		Assert.notNull(processManager, "Process manager required");
		this.processManager = processManager;
	}

	public void onApplicationEvent(ContextRefreshedEvent event) {
		Assert.notNull(event, "Application event required");
		processManager.completeStartup();
	}

}
