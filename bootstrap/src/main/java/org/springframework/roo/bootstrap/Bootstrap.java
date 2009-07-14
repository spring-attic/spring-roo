package org.springframework.roo.bootstrap;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Loads a {@link Shell} using Spring IoC container.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class Bootstrap {

	private static Bootstrap bootstrap;
	private Shell shell;
	private ConfigurableApplicationContext ctx;
	
	public static void main(String[] args) throws IOException {
        String applicationContextLocation = "classpath:roo-bootstrap.xml";
        if (args.length > 0) {
        	applicationContextLocation = args[0];
        }
        
        StringBuilder sb = new StringBuilder();
        if (args.length > 1) {
        	for (int i = 1; i < args.length; i++) {
        		if (i >= 2) {
        			sb.append(" ");
        		}
        		sb.append(args[i]);
        	}
        }
        String executeThenQuit = sb.toString();
        
        boolean successful = true;
        try {
        	bootstrap = new Bootstrap(applicationContextLocation);
            successful = bootstrap.run(executeThenQuit);
        } catch (RuntimeException t) {
        	successful = false;
        	throw t;
        } finally {
    		HandlerUtils.flushAllHandlers(Logger.getLogger(""));
        }
        
        if (successful) {
        	System.exit(0);
        }
        System.exit(1);
    }

	public Bootstrap(String applicationContextLocation) {
		Assert.hasText(applicationContextLocation, "Application context location required");
        
		setupLogging();

        ctx = new ClassPathXmlApplicationContext(applicationContextLocation);
        
        Map<String,Shell> shells = ctx.getBeansOfType(Shell.class);

        Assert.isTrue(shells.size() == 1, "Exactly one Shell was required, but " + shells.size() + " found");
        shell = shells.values().iterator().next();
        
        Map<String,CommandMarker> commands = ctx.getBeansOfType(CommandMarker.class);
        
        for (CommandMarker bean : commands.values()) {
        	shell.getParser().addTarget(bean);
        }
        
        Map<String,Converter> converters = ctx.getBeansOfType(Converter.class);
        
        for (Converter bean : converters.values()) {
        	shell.getParser().addConverter(bean);
        }

	}

	private void setupLogging() {
		// Ensure all JDK log messages are deferred until a target is registered
		Logger rootLogger = Logger.getLogger("");
		HandlerUtils.wrapWithDeferredLogHandler(rootLogger, Level.SEVERE);
		
		// Set a suitable priority level on Spring Framework log messages
		Logger sfwLogger = Logger.getLogger("org.springframework");
		sfwLogger.setLevel(Level.WARNING);

		// Set a suitable priority level on ROO log messages
		Logger rooLogger = Logger.getLogger("org.springframework.roo");
		rooLogger.setLevel(Level.FINE);
}
	
	protected boolean run(String executeThenQuit) {
        boolean successful = true;
		if (!"".equals(executeThenQuit)) {
        	successful = shell.executeCommand(executeThenQuit);
        	shell.requestExit();
        } else {
           	shell.promptLoop();
        }
        
        ctx.close();
        return successful;
	}
}
