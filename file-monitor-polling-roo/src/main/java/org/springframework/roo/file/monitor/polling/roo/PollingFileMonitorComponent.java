package org.springframework.roo.file.monitor.polling.roo;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.polling.PollingFileMonitorService;

/**
 * Extends {@link PollingFileMonitorService} by making it available as an OSGi component
 * that automatically monitors the environment's {@link FileEventListener} components.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
@Reference(name = "fileEventListener", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = FileEventListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE) 
public class PollingFileMonitorComponent extends PollingFileMonitorService {

	protected void bindFileEventListener(FileEventListener listener) {
		add(listener);
	}
	
	protected void unbindFileEventListener(FileEventListener listener) {
		remove(listener);
	}

}
