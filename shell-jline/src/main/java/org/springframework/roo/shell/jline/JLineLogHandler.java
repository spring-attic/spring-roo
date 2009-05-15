package org.springframework.roo.shell.jline;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jline.ANSIBuffer;
import jline.ConsoleReader;

import org.springframework.roo.shell.ShellPromptAccessor;
import org.springframework.roo.support.util.Assert;

/**
 * JDK logging {@link Handler} that emits log messages to a JLine {@link ConsoleReader}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JLineLogHandler extends Handler {

	private ConsoleReader reader;
	private ShellPromptAccessor shellPromptAccessor;
	private static ThreadLocal<Boolean> redrawProhibit = new ThreadLocal<Boolean>();
	
	public JLineLogHandler(ConsoleReader reader, ShellPromptAccessor shellPromptAccessor) {
		Assert.notNull(reader, "Console reader required");
		Assert.notNull(shellPromptAccessor, "Shell prompt accessor required");
		this.reader = reader;
		this.shellPromptAccessor = shellPromptAccessor;
		
		setFormatter(new Formatter() {
			public String format(LogRecord record) {
				StringBuffer sb = new StringBuffer();
				
				if (record.getMessage() != null) {
					sb.append(record.getMessage()).append(System.getProperty("line.separator"));
				}
				
				if (record.getThrown() != null) {
				    try {
				        StringWriter sw = new StringWriter();
				        PrintWriter pw = new PrintWriter(sw);
				        record.getThrown().printStackTrace(pw);
				        pw.close();
					sb.append(sw.toString());
				    } catch (Exception ex) {
				    }
				}
				return sb.toString();
			}
		});
		
	}

	@Override
	public void flush() {}

	@Override
	public void close() throws SecurityException {}

	public static void prohibitRedraw() {
		redrawProhibit.set(true);
	}
	
	public static void cancelRedrawProhibition() {
		redrawProhibit.remove();
	}

	@Override
	public void publish(LogRecord record) {
		try {
			StringBuffer buffer = reader.getCursorBuffer().getBuffer();
			int cursor = reader.getCursorBuffer().cursor;
			if (reader.getCursorBuffer().length() > 0) {
				// The user has semi-typed something, so put a new line in so the debug message is separated
				reader.printNewline();
				
				// We need to cancel whatever they typed (it's reset later on), so the line appears empty
				reader.getCursorBuffer().setBuffer(new StringBuffer());
				reader.getCursorBuffer().cursor = 0;
			}

			// This ensures nothing is ever displayed when redrawing the line
			reader.setDefaultPrompt("");
			reader.redrawLine();
			
			// Now restore the line formatting settings back to their original
			reader.setDefaultPrompt(shellPromptAccessor.getShellPrompt());

			reader.getCursorBuffer().setBuffer(buffer);
			reader.getCursorBuffer().cursor = cursor;

			String toDisplay = toDisplay(record, reader.getTerminal().isANSISupported());
			reader.printString(toDisplay);
			
			Boolean prohibitingRedraw = redrawProhibit.get();
			if (prohibitingRedraw == null) {
				reader.redrawLine();
			}
			
			reader.flushConsole();
		} catch (Exception e) {
			reportError("Could not publish log message", e, Level.SEVERE.intValue());
		}
	}

	private String toDisplay(LogRecord event, boolean ansiSupported) {
	    StringBuilder sb = new StringBuilder();
		
		if (ansiSupported) {
			if (event.getLevel().intValue() >= Level.SEVERE.intValue()) {
				sb.append(new ANSIBuffer().red(getFormatter().format(event)));
			} else if (event.getLevel().intValue() >= Level.WARNING.intValue()) {
				sb.append(new ANSIBuffer().magenta(getFormatter().format(event)));
			} else if (event.getLevel().intValue() >= Level.INFO.intValue()) {
				sb.append(new ANSIBuffer().green(getFormatter().format(event)));
			} else {
				sb.append(getFormatter().format(event));
			}
		} else {
			sb.append(getFormatter().format(event));
		}
	    
		return sb.toString();
	}
	
}
