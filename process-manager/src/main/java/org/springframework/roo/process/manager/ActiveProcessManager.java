package org.springframework.roo.process.manager;

/**
 * Guarantees to provide access to the currently-executing {@link ProcessManager}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class ActiveProcessManager {
	private static ThreadLocal<ProcessManager> processManager = new ThreadLocal<ProcessManager>();
	
	public static ProcessManager getActiveProcessManager() {
		return processManager.get();
	}
	
	public static void setActiveProcessManager(ProcessManager pm) {
		processManager.set(pm);
	}
	
	public static void clearActiveProcessManager() {
		processManager.remove();
	}
}
