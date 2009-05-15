package org.springframework.roo.process.manager.internal;

import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.MonitoringRequestEditor;
import org.springframework.roo.support.util.Assert;

/**
 * A simple {@link MonitoringRequest} wrapper.
 * 
 * <p>
 * This class is primarily so that we can configure a simple object in the Spring application context via the
 * {@link MonitoringRequestEditor} class. It serves no other useful purpose.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class InitialMonitoringRequest {

	private MonitoringRequest monitoringRequest;

	public InitialMonitoringRequest(MonitoringRequest monitoringRequest) {
		Assert.notNull(monitoringRequest, "Monitoring request required");
		this.monitoringRequest = monitoringRequest;
	}

	public MonitoringRequest getMonitoringRequest() {
		return monitoringRequest;
	}
	
}
