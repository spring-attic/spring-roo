package org.springframework.roo.shell;

/**
 * An immutable representation of a request to exit the shell.
 * 
 * <p>
 * Implementations of the shell are free to handle these requests in whatever
 * way they wish. Callers should not expect an exit request to be completed.
 * 
 * @author balex
 *
 */
public final class ExitShellRequest {

	private int exitCode;
	
	public static final ExitShellRequest NORMAL_EXIT = new ExitShellRequest(0);
	public static final ExitShellRequest FATAL_EXIT = new ExitShellRequest(1);
	public static final ExitShellRequest EXIT_AND_RESTART = new ExitShellRequest(100);

	private ExitShellRequest(int exitCode) {
		this.exitCode = exitCode;
	}

	public int getExitCode() {
		return exitCode;
	}
	
}
