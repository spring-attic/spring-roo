package org.springframework.roo.felix;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Delegates OSGi log messages to the JDK logging infrastructure. This in turn makes it compatible
 * with Spring Roo's standard approach to log messages appearing on the console.
 * 
 * <p>
 * For convenience all low-priority messages are output as flash messages. All high priority messages
 * are sent to the JDK logger.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component(immediate=true)
@Reference(name="shell", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=Shell.class, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
public class JdkDelegatingLogListener extends AbstractFlashingObject implements LogListener {

	@Reference private LogReaderService logReaderService;
	private final static Logger logger = HandlerUtils.getLogger(JdkDelegatingLogListener.class);
	
	@SuppressWarnings("unchecked")
	protected void activate(ComponentContext context) {
		logReaderService.addLogListener(this);
		Enumeration<LogEntry> latestLogs = logReaderService.getLog();
		if (latestLogs.hasMoreElements()) {
			logNow(latestLogs.nextElement());
		}
	}

	protected void deactivate(ComponentContext context) {
		logReaderService.removeLogListener(this);
	}
	
	public void logged(LogEntry entry) {
		logNow(entry);
	}

	private void logNow(LogEntry entry) {
		int osgiLevel = entry.getLevel();
		Level jdkLevel = Level.FINEST;
		
		// Convert the OSGi level into a JDK logger level
		if (osgiLevel == LogService.LOG_DEBUG) {
			jdkLevel = Level.FINE;
		} else if (osgiLevel == LogService.LOG_INFO) {
			jdkLevel = Level.INFO;
		} else if (osgiLevel == LogService.LOG_WARNING) {
			jdkLevel = Level.WARNING;
		} else if (osgiLevel == LogService.LOG_ERROR) {
			jdkLevel = Level.SEVERE;
		}
		
		if (jdkLevel.intValue() <= Level.INFO.intValue()) {
			// Not very important message, so just flash it if possible and we're in development mode
			if (isDevelopmentMode()) {
				flash(jdkLevel, buildMessage(entry), MY_SLOT);
				// Immediately clear it once the timeout has been reached
				flash(jdkLevel, "", MY_SLOT);
			}
		} else {
			// Important log message, so log it via JDK
			logger.log(jdkLevel, buildMessage(entry), entry.getException());
		}
	}
	
	private String buildMessage(LogEntry entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(entry.getBundle()).append("] ").append(entry.getMessage());
		return sb.toString();
	}

}
