package org.springframework.roo.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.springframework.shell.core.AbstractShell;
import org.springframework.shell.core.ExecutionStrategy;
import org.springframework.shell.core.ExitShellRequest;
import org.springframework.shell.event.ParseResult;
import org.springframework.shell.event.ShellStatus;
import org.springframework.shell.event.ShellStatus.Status;

/**
 * 
 * Spring Roo custom implementation of Spring Shell AbstractShell
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 *
 */
@Component
public abstract class RooAbstractShell extends AbstractShell implements
		RooShell {

	private static final String MY_SLOT = AbstractShell.class.getName();

	List<CommandListener> commandListeners = new ArrayList<CommandListener>();

	private CommandListener commandListener;

	@Override
	public void addListerner(CommandListener listener) {
		commandListeners.add(listener);
	}

	@Override
	public void removeListener(CommandListener listener) {
		commandListeners.remove(listener);
	}

	private void notifyExecutionFailed() {
		if (commandListeners.isEmpty()) {
			return;
		}
		for (CommandListener listener : commandListeners) {
			listener.onCommandFails();
		}
	}

	private void notifyExecutionSuccess() {
		if (commandListeners.isEmpty()) {
			return;
		}
		for (CommandListener listener : commandListeners) {
			listener.onCommandSuccess();
		}
	}

	private void notifyBeginExecute(ParseResult parseResult) {
		if (commandListeners.isEmpty()) {
			return;
		}
		for (CommandListener listener : commandListeners) {
			listener.onCommandBegin(parseResult);
		}
	}

	/**
	 * Runs the specified command. Control will return to the caller after the
	 * command is run.
	 */
	private boolean executeCommandImpl(String line) {
		// Another command was attempted
		setShellStatus(ShellStatus.Status.PARSING);
		final ExecutionStrategy executionStrategy = getExecutionStrategy();
		boolean flashedMessage = false;
		while (executionStrategy == null
				|| !executionStrategy.isReadyForCommands()) {
			// Wait
			try {
				Thread.sleep(500);
			} catch (final InterruptedException ignore) {
			}
			if (!flashedMessage) {
				flash(Level.INFO, "Please wait - still loading", MY_SLOT);
				flashedMessage = true;
			}
		}
		if (flashedMessage) {
			flash(Level.INFO, "", MY_SLOT);
		}
		ParseResult parseResult = null;
		try {
			// We support simple block comments; ie a single pair per line
			if (!inBlockComment && line.contains("/*") && line.contains("*/")) {
				blockCommentBegin();
				final String lhs = line.substring(0, line.lastIndexOf("/*"));
				if (line.contains("*/")) {
					line = lhs + line.substring(line.lastIndexOf("*/") + 2);
					blockCommentFinish();
				} else {
					line = lhs;
				}
			}
			if (inBlockComment) {
				if (!line.contains("*/")) {
					return true;
				}
				blockCommentFinish();
				line = line.substring(line.lastIndexOf("*/") + 2);
			}
			// We also support inline comments (but only at start of line,
			// otherwise valid
			// command options like http://www.helloworld.com will fail as per
			// ROO-517)
			if (!inBlockComment
					&& (line.trim().startsWith("//") || line.trim().startsWith(
							"#"))) { // # support in ROO-1116
				line = "";
			}
			// Convert any TAB characters to whitespace (ROO-527)
			line = line.replace('\t', ' ');
			if ("".equals(line.trim())) {
				setShellStatus(Status.EXECUTION_SUCCESS);
				return true;
			}
			parseResult = getParser().parse(line);
			if (parseResult == null) {
				return false;
			}
			try {
				notifyBeginExecute(parseResult);
			} catch (final Exception ignored) {
			}
			setShellStatus(Status.EXECUTING);
			final Object result = executionStrategy.execute(parseResult);
			setShellStatus(Status.EXECUTION_RESULT_PROCESSING);
			if (result != null) {
				if (result instanceof ExitShellRequest) {
					exitShellRequest = (ExitShellRequest) result;
					// Give ProcessManager a chance to close down its threads
					// before the overall OSGi framework is terminated
					// (ROO-1938)
					executionStrategy.terminate();
				} else if (result instanceof Iterable<?>) {
					for (final Object o : (Iterable<?>) result) {
						logger.info(o.toString());
					}
				} else {
					logger.info(result.toString());
				}
			}
			logCommandIfRequired(line, true);
			setShellStatus(Status.EXECUTION_SUCCESS, line, parseResult);
			// ROO-3581: When command success, execute command listener SUCCESS
			try {
				notifyExecutionSuccess();
			} catch (final Exception ignored) {
			}
			return true;
		} catch (final RuntimeException e) {
			setShellStatus(Status.EXECUTION_FAILED, line, parseResult);
			try {
				// ROO-3581: When command fails, execute command listener FAILS
				notifyExecutionFailed();
			} catch (final Exception ignored) {
			}
			// We rely on execution strategy to log it
			try {
				logCommandIfRequired(line, false);
			} catch (final Exception ignored) {
			}
			return false;
		} finally {
			setShellStatus(Status.USER_INPUT);
		}
	}

}