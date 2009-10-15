package org.springframework.roo.shell.jline;

import java.io.IOException;
import java.util.logging.Logger;

import jline.ANSIBuffer;
import jline.ConsoleReader;

import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.SimpleParser;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.internal.AbstractShell;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.logging.DeferredLogHandler;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Uses the feature-rich <a href="http://jline.sourceforge.net/">JLine</a> library to provide an interactive shell.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class JLineShell extends AbstractShell implements Shell {

    private ConsoleReader reader;
    private SimpleParser parser;
    private boolean developmentMode = false;
    
	public JLineShell(ExecutionStrategy executionStrategy) {
		super(executionStrategy);
		
		try {
			reader = new ConsoleReader();
		} catch (IOException ioe) {
			throw new IllegalStateException("Cannot start console class", ioe);
		}
		
        if (reader.getTerminal().isANSISupported()) {
        	ANSIBuffer ansi = new ANSIBuffer();
        	shellPrompt = ansi.yellow("roo> ").toString();
        }
        
        JLineLogHandler handler = new JLineLogHandler(reader, this);
        JLineLogHandler.prohibitRedraw(); // affects this thread only
        int detected = HandlerUtils.registerTargetHandler(Logger.getLogger(""), handler);
        Assert.isTrue(detected > 0, "The default logger failed to provide any " + DeferredLogHandler.class.getName() + " instances");

        parser = new SimpleParser();
        reader.addCompletor(new JLineCompletorAdapter(parser));
		parser.addTarget(this);
		
        reader.setDefaultPrompt(JLineShell.shellPrompt);
		reader.setBellEnabled(true);
		if (Boolean.getBoolean("jline.nobell")) {
        	reader.setBellEnabled(false);
		}
                
		// reader.setDebug(new PrintWriter(new FileWriter("writer.debug", true)));

        logger.info(version(null));
        logger.info("Welcome to Spring Roo. For assistance press " + completionKeys + " or type \"hint\" then hit ENTER.");
        
        setShellStatus(ShellStatus.STARTED);
	}
	
    public void promptLoop() {
    	setShellStatus(ShellStatus.USER_INPUT);
    	String line;
        try {
            while (exitShellRequest == null && ( (line = reader.readLine() ) != null) ) {
            	JLineLogHandler.resetMessageTracking();
            	setShellStatus(ShellStatus.USER_INPUT);

            	if ("".equals(line)) {
                	continue;
                }
                
                executeCommand(line);
            }
        } catch (IOException ioe) {
        	throw new IllegalStateException("Shell line reading failure", ioe);
        }
        
        setShellStatus(ShellStatus.SHUTTING_DOWN);
    }
    
	public SimpleParser getParser() {
		return parser;
	}

	public void setDevelopmentMode(boolean developmentMode) {
		JLineLogHandler.setIncludeThreadName(developmentMode);
		this.developmentMode = developmentMode;
	}

	public boolean isDevelopmentMode() {
		return this.developmentMode;
	}

}
