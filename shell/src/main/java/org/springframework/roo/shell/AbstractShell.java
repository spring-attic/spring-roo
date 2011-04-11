package org.springframework.roo.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import org.springframework.roo.shell.event.AbstractShellStatusPublisher;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatus.Status;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Provides a base {@link Shell} implementation.
 * 
 * @author Ben Alex
 */
public abstract class AbstractShell extends AbstractShellStatusPublisher implements Shell {
	private static final String MY_SLOT = AbstractShell.class.getName();
	protected final Logger logger = HandlerUtils.getLogger(getClass());
    protected boolean inBlockComment = false;
    protected ExitShellRequest exitShellRequest = null;
	public static String shellPrompt = "roo> ";
	public static String completionKeys = "TAB";
	
	protected abstract Set<URL> findUrls(String resourceName);
	protected abstract String getHomeAsString();
	protected abstract ExecutionStrategy getExecutionStrategy();
	protected abstract Parser getParser();

	@CliCommand(value = { "script" }, help = "Parses the specified resource file and executes its commands")
	public void script(
		@CliOption(key = { "", "file" }, help = "The file to locate and execute", mandatory = true) File resource, 
		@CliOption(key = "lineNumbers", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Display line numbers when executing the script") boolean lineNumbers) {
		
		Assert.notNull(resource, "Resource to parser is required");
		long started = new Date().getTime();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(resource);
		} catch (FileNotFoundException tryTheClassLoaderInstead) {}
		
		if (inputStream == null) {
			// Try to find the resource via the classloader
			Set<URL> urls = findUrls(resource.getName());
			
			// Handle search system failure
			Assert.notNull(urls, "Unable to process classpath bundles to locate the script");
			
			// Handle the file simply not being present, but the search being OK
			Assert.notEmpty(urls, "Resource '" + resource + "' not found on disk or in classpath");
			Assert.isTrue(urls.size() == 1, "More than one '" + resource + "' was found in the classpath; unable to continue");
			try {
				inputStream = urls.iterator().next().openStream();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			int i = 0;
			while ((line = in.readLine()) != null) {
				i++;
				if (lineNumbers) {
					logger.fine("Line " + i + ": " + line);
				} else {
					logger.fine(line);
				}
				if (!"".equals(line.trim())) {
					boolean success = executeScriptLine(line);
					if (success && ((line.trim().startsWith("q") || line.trim().startsWith("ex")))) {
						break;
					} else if (!success) {
						// Abort script processing, given something went wrong
						throw new IllegalStateException("Script execution aborted");
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignored) {}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignored) {}
			}
			
			logger.fine("Script required " + ((new Date().getTime() - started) / 1000) + " second(s) to execute");
		}
	}
	
	/**
	 * Execute the single line from a script.
	 * <p>
	 * This method can be overridden by sub-classes to pre-process script lines. 
	 */
	protected boolean executeScriptLine(String line) {
		return executeCommand(line);
	}
	
	public boolean executeCommand(String line) {
		// Another command was attempted
    	setShellStatus(ShellStatus.Status.PARSING);

    	ExecutionStrategy executionStrategy = getExecutionStrategy();
    	boolean flashedMessage = false;
    	while (executionStrategy == null || !executionStrategy.isReadyForCommands()) {
    		// Wait
    		try {
				Thread.sleep(500);
			} catch (InterruptedException ignore) {}
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
				String lhs = line.substring(0, line.lastIndexOf("/*"));
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
			// We also support inline comments (but only at start of line, otherwise valid
			// command options like http://www.helloworld.com will fail as per ROO-517)
			if (!inBlockComment && (line.trim().startsWith("//") || line.trim().startsWith("#"))) { // # support in ROO-1116
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
			
			setShellStatus(Status.EXECUTING);
			Object result = executionStrategy.execute(parseResult);
			setShellStatus(Status.EXECUTION_RESULT_PROCESSING);
			if (result != null) {
				if (result instanceof ExitShellRequest) {
					exitShellRequest = (ExitShellRequest) result;
					// Give ProcessManager a chance to close down its threads before the overall OSGi framework is terminated (ROO-1938)
					executionStrategy.terminate();
				} else if (result instanceof Iterable<?>) {
					for (Object o : (Iterable<?>) result) {
						logger.info(o.toString());
					}
				} else {
					logger.info(result.toString());
				}
			}
			
			logCommandIfRequired(line, true);
			setShellStatus(Status.EXECUTION_SUCCESS, line, parseResult);
			return true;
		} catch (RuntimeException e) {
	    	setShellStatus(Status.EXECUTION_FAILED, line, parseResult);
			// We rely on execution strategy to log it
			// Throwable root = ExceptionUtils.extractRootCause(ex);
			// logger.log(Level.FINE, root.getMessage());
	    	try {
	    		logCommandIfRequired(line, false);
	    	} catch (Exception ignored) {}
			return false;
		} finally {
			setShellStatus(Status.USER_INPUT);
		}
	}
	
	/**
	 * Allows a subclass to log the execution of a well-formed command. This is invoked after a command
	 * has completed, and indicates whether the command returned normally or returned an exception. Note
	 * that attempted commands that are not well-formed (eg they are missing a mandatory argument) will
	 * never be presented to this method, as the command execution is never actually attempted in those
	 * cases. This method is only invoked if an attempt is made to execute a particular command.
	 * 
	 * <p>
	 * Implementations should consider specially handling the "script" commands, and also
	 * indicating whether a command was successful or not. Implementations that wish to behave
	 * consistently with other {@link AbstractShell} subclasses are encouraged to simply override
	 * {@link #logCommandToOutput(String)} instead, and only override this method if you actually
	 * need to fine-tune the output logic.
	 *  
	 * @param line the parsed line (any comments have been removed; never null)
	 * @param successful if the command was successful or not
	 */
	protected void logCommandIfRequired(String line, boolean successful) {
		if (line.startsWith("script")) {
			logCommandToOutput((successful ? "// " : "// [failed] ") + line);
		} else {
			logCommandToOutput((successful ? "" : "// [failed] ") + line);
		}
	}
	
	/**
	 * Allows a subclass to actually write the resulting logged command to some form of output. This
	 * frees subclasses from needing to implement the logic within {@link #logCommandIfRequired(String, boolean)}.
	 *
	 * <p>
	 * Implementations should invoke {@link #getExitShellRequest()} to monitor any attempts to exit the shell and
	 * release resources such as output log files.
	 * 
	 * @param processedLine the line that should be appended to some type of output (excluding the \n character)
	 */
	protected void logCommandToOutput(String processedLine) {
		// logger.severe(processedLine);
	}

	/**
	 * Base implementation of the {@link Shell#setPromptPath(String)} method, designed for simple shell
	 * implementations. Advanced implementations (eg those that support ANSI codes etc) will likely want
	 * to override this method and set the {@link #shellPrompt} variable directly.
	 * 
	 * @param path to set (can be null or empty; must NOT be formatted in any special way eg ANSI codes)
	 */
	public void setPromptPath(String path) {
		if ("".equals(path) || path == null) {
			shellPrompt = "roo> ";
		} else {
			shellPrompt = path + " roo> ";
		}
	}

	public ExitShellRequest getExitShellRequest() {
		return exitShellRequest;
	}

	@CliCommand(value = { "//", ";" }, help = "Inline comment markers (start of line only)")
	public void inlineComment() {}

	@CliCommand(value = { "/*" }, help = "Start of block comment")
	public void blockCommentBegin() {
		Assert.isTrue(!inBlockComment, "Cannot open a new block comment when one already active");
		inBlockComment = true;
	}

	@CliCommand(value = { "*/" }, help = "End of block comment")
	public void blockCommentFinish() {
		Assert.isTrue(inBlockComment, "Cannot close a block comment when it has not been opened");
		inBlockComment = false;
	}

	@CliCommand(value = { "system properties" }, help = "Shows the shell's properties")
	public String props() {
		Properties properties = System.getProperties();
		SortedSet<String> data = new TreeSet<String>();
		for (Object property : properties.keySet()) {
			Object value = properties.get(property);
			data.add(property + " = " + value);
		}
		
		StringBuilder sb = new StringBuilder();
		for (String line : data) {
			sb.append(line).append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	@CliCommand(value = { "date" }, help = "Displays the local date and time")
	public String date() {
		return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date());
	}

	@CliCommand(value={"flash test"}, help="Tests message flashing")
	public void flashCustom() throws Exception {
		flash(Level.FINE, "Hello world", "a");
		Thread.sleep(150);
		flash(Level.FINE, "Short world", "a");
		Thread.sleep(150);
		flash(Level.FINE, "Small", "a");
		Thread.sleep(150);
		flash(Level.FINE, "Downloading xyz", "b");
		Thread.sleep(150);
		flash(Level.FINE, "", "a");
		Thread.sleep(150);
		flash(Level.FINE, "Downloaded xyz", "b");
		Thread.sleep(150);
		flash(Level.FINE, "System online", "c");
		Thread.sleep(150);
		flash(Level.FINE, "System ready", "c");
		Thread.sleep(150);
		flash(Level.FINE, "System farewell", "c");
		Thread.sleep(150);
		flash(Level.FINE, "", "c");
		Thread.sleep(150);
		flash(Level.FINE, "", "b");
	}
	
	@CliCommand(value={"version"}, help="Displays shell version")
	public String version(@CliOption(key="", help="Special version flags") String extra) {
    	StringBuilder sb = new StringBuilder();
		
    	if ("jaime".equals(extra)) {
    		sb.append("               /\\ /l").append(System.getProperty("line.separator"));
    		sb.append("               ((.Y(!").append(System.getProperty("line.separator"));
    		sb.append("                \\ |/").append(System.getProperty("line.separator"));
    		sb.append("                /  6~6,").append(System.getProperty("line.separator"));
    		sb.append("                \\ _    +-.").append(System.getProperty("line.separator"));
    		sb.append("                 \\`-=--^-' \\").append(System.getProperty("line.separator"));
    		sb.append("                  \\   \\     |\\--------------------------+").append(System.getProperty("line.separator"));
    		sb.append("                 _/    \\    |  Thanks for loading Roo!  |").append(System.getProperty("line.separator"));
    		sb.append("                (  .    Y   +---------------------------+").append(System.getProperty("line.separator"));
    		sb.append("               /\"\\ `---^--v---.").append(System.getProperty("line.separator"));
    		sb.append("              / _ `---\"T~~\\/~\\/").append(System.getProperty("line.separator"));
    		sb.append("             / \" ~\\.      !").append(System.getProperty("line.separator"));
    		sb.append("       _    Y      Y.~~~ /'").append(System.getProperty("line.separator"));
    		sb.append("      Y^|   |      | Roo 7").append(System.getProperty("line.separator"));
    		sb.append("      | l   |     / .   /'").append(System.getProperty("line.separator"));
    		sb.append("      | `L  | Y .^/   ~T").append(System.getProperty("line.separator"));
    		sb.append("      |  l  ! | |/  | |               ____  ____  ____").append(System.getProperty("line.separator"));
    		sb.append("      | .`\\/' | Y   | !              / __ \\/ __ \\/ __ \\").append(System.getProperty("line.separator"));
    		sb.append("      l  \"~   j l   j L______       / /_/ / / / / / / /").append(System.getProperty("line.separator"));
    		sb.append("       \\,____{ __\"\" ~ __ ,\\_,\\_    / _, _/ /_/ / /_/ /").append(System.getProperty("line.separator"));
    		sb.append("    ~~~~~~~~~~~~~~~~~~~~~~~~~~~   /_/ |_|\\____/\\____/").append(" ").append(versionInfo()).append(System.getProperty("line.separator"));
    		return sb.toString();
    	}
    	
    	sb.append("    ____  ____  ____  ").append(System.getProperty("line.separator")); 
		sb.append("   / __ \\/ __ \\/ __ \\ ").append(System.getProperty("line.separator"));
		sb.append("  / /_/ / / / / / / / ").append(System.getProperty("line.separator"));
		sb.append(" / _, _/ /_/ / /_/ /  ").append(System.getProperty("line.separator"));
		sb.append("/_/ |_|\\____/\\____/   ").append(" ").append(versionInfo()).append(System.getProperty("line.separator"));
		sb.append(System.getProperty("line.separator"));
		
		return sb.toString();
	}
	
	public static String versionInfo() {
		// Try to determine the bundle version
		String bundleVersion = null;
		String gitCommitHash = null;
		JarFile jarFile = null;
		try {
			URL classContainer = AbstractShell.class.getProtectionDomain().getCodeSource().getLocation();
			if (classContainer.toString().endsWith(".jar")) {
				// Attempt to obtain the "Bundle-Version" version from the manifest
				jarFile = new JarFile(new File(classContainer.toURI()), false);
				ZipEntry manifestEntry = jarFile.getEntry("META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(jarFile.getInputStream(manifestEntry));
				bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
				gitCommitHash = manifest.getMainAttributes().getValue("Git-Commit-Hash");
			}
		} catch (Exception ignoreAndMoveOn) { }
		finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				}
				catch (IOException ignored) {}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		
		if (bundleVersion != null) {
			sb.append(bundleVersion);
		}
		
		if (gitCommitHash != null && gitCommitHash.length() > 7) {
			if (sb.length() > 0) {
				sb.append(" "); // to separate from version
			}
			sb.append("[rev ");
			sb.append(gitCommitHash.substring(0,7));
			sb.append("]");
		}
		
		if (sb.length() == 0) {
			sb.append("UNKNOWN VERSION");
		}
		
		return sb.toString();
	}

	public String getShellPrompt() {
		return shellPrompt;
	}
	
	/**
	 * Obtains the home directory for the current shell instance.
	 *
	 * <p>
	 * Note: calls the {@link #getHomeAsString()} method to allow subclasses to provide the home directory location as 
	 * string using different environment-specific strategies. 
 	 *
	 * <p>
	 * If the path indicated by {@link #getHomeAsString()} exists and refers to a directory, that directory
	 * is returned.
	 * 
	 * <p>
	 * If the path indicated by {@link #getHomeAsString()} exists and refers to a file, an exception is thrown.
	 * 
	 * <p>
	 * If the path indicated by {@link #getHomeAsString()} does not exist, it will be created as a directory.
	 * If this fails, an exception will be thrown.
	 * 
	 * @return the home directory for the current shell instance (which is guaranteed to exist and be a directory)
	 */
	public File getHome() {
		String rooHome = getHomeAsString();
		File f = new File(rooHome);
		Assert.isTrue(!f.exists() || (f.exists() && f.isDirectory()), "Path '" + f.getAbsolutePath() + "' must be a directory, or it must not exist");
		if (!f.exists()) {
			f.mkdirs();
		}
		Assert.isTrue(f.exists() && f.isDirectory(), "Path '" + f.getAbsolutePath() + "' is not a directory; please specify roo.home system property correctly");
		return f;
	}

	/**
	 * Simple implementation of {@link #flash(Level, String, String)} that simply displays the message via the logger. It is
	 * strongly recommended shell implementations override this method with a more effective approach.
	 */
	public void flash(Level level, String message, String slot) {
		Assert.notNull(level, "Level is required for a flash message");
		Assert.notNull(message, "Message is required for a flash message");
		Assert.hasText(slot, "Slot name must be specified for a flash message");
		if (!("".equals(message))) {
			logger.log(level, message);
		}
	}
}
