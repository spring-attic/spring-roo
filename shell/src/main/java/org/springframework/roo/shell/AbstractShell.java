package org.springframework.roo.shell;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.shell.event.AbstractShellStatusPublisher;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatus.Status;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Provides a base {@link Shell} implementation.
 * 
 * @author Ben Alex
 */
public abstract class AbstractShell extends AbstractShellStatusPublisher
        implements Shell {

    private static final String MY_SLOT = AbstractShell.class.getName();
    protected static final String ROO_PROMPT = "roo> ";

    // Public static fields; don't rename, make final, or make non-public, as
    // they are part of the public API, e.g. are changed by STS.
    public static String completionKeys = "TAB";
    public static String shellPrompt = ROO_PROMPT;

    public static String versionInfo() {
        // Try to determine the bundle version
        String bundleVersion = null;
        String gitCommitHash = null;
        JarFile jarFile = null;
        try {
            final URL classContainer = AbstractShell.class
                    .getProtectionDomain().getCodeSource().getLocation();
            if (classContainer.toString().endsWith(".jar")) {
                // Attempt to obtain the "Bundle-Version" version from the
                // manifest
                jarFile = new JarFile(new File(classContainer.toURI()), false);
                final ZipEntry manifestEntry = jarFile
                        .getEntry("META-INF/MANIFEST.MF");
                final Manifest manifest = new Manifest(
                        jarFile.getInputStream(manifestEntry));
                bundleVersion = manifest.getMainAttributes().getValue(
                        "Bundle-Version");
                gitCommitHash = manifest.getMainAttributes().getValue(
                        "Git-Commit-Hash");
            }
        }
        catch (final IOException ignoreAndMoveOn) {
        }
        catch (final URISyntaxException ignoreAndMoveOn) {
        }
        finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                }
                catch (final IOException ignored) {
                }
            }
        }

        final StringBuilder sb = new StringBuilder();

        if (bundleVersion != null) {
            sb.append(bundleVersion);
        }

        if (gitCommitHash != null && gitCommitHash.length() > 7) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("[rev ");
            sb.append(gitCommitHash.substring(0, 7));
            sb.append("]");
        }

        if (sb.length() == 0) {
            sb.append("UNKNOWN VERSION");
        }

        return sb.toString();
    }

    protected final Logger logger = HandlerUtils.getLogger(getClass());
    protected boolean inBlockComment;
    protected ExitShellRequest exitShellRequest;
    private Tailor tailor;

    @CliCommand(value = { "/*" }, help = "Start of block comment")
    public void blockCommentBegin() {
        Validate.isTrue(!inBlockComment,
                "Cannot open a new block comment when one already active");
        inBlockComment = true;
    }

    @CliCommand(value = { "*/" }, help = "End of block comment")
    public void blockCommentFinish() {
        Validate.isTrue(inBlockComment,
                "Cannot close a block comment when it has not been opened");
        inBlockComment = false;
    }

    @CliCommand(value = { "date" }, help = "Displays the local date and time")
    public String date() {
        return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL)
                .format(new Date());
    }

    public boolean executeCommand(final String line) {
        if (tailor == null) {
            return executeCommandImpl(line);
        }
        /*
         * If getTailor() is not null, then try to transform input command and
         * execute all outputs sequentially
         */
        List<String> commands = null;
        commands = tailor.sew(line);

        if (CollectionUtils.isEmpty(commands)) {
            return executeCommandImpl(line);
        }
        for (final String command : commands) {
            logger.info("roo-tailor> " + command);
            if (!executeCommandImpl(command)) {
                return false;
            }
        }
        return true;
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
            }
            catch (final InterruptedException ignore) {
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
                }
                else {
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
                }
                else if (result instanceof Iterable<?>) {
                    for (final Object o : (Iterable<?>) result) {
                        logger.info(o.toString());
                    }
                }
                else {
                    logger.info(result.toString());
                }
            }

            logCommandIfRequired(line, true);
            setShellStatus(Status.EXECUTION_SUCCESS, line, parseResult);
            return true;
        }
        catch (final RuntimeException e) {
            setShellStatus(Status.EXECUTION_FAILED, line, parseResult);
            // We rely on execution strategy to log it
            try {
                logCommandIfRequired(line, false);
            }
            catch (final Exception ignored) {
            }
            return false;
        }
        finally {
            setShellStatus(Status.USER_INPUT);
        }
    }

    /**
     * Execute the single line from a script.
     * <p>
     * This method can be overridden by sub-classes to pre-process script lines.
     */
    protected boolean executeScriptLine(final String line) {
        return executeCommand(line);
    }

    /**
     * Returns any classpath resources with the given path
     * 
     * @param path the path for which to search (never null)
     * @return <code>null</code> if the search can't be performed
     * @since 1.2.0
     */
    protected abstract Collection<URL> findResources(String path);

    /**
     * Simple implementation of {@link #flash(Level, String, String)} that
     * simply displays the message via the logger. It is strongly recommended
     * shell implementations override this method with a more effective
     * approach.
     */
    public void flash(final Level level, final String message, final String slot) {
        Validate.notNull(level, "Level is required for a flash message");
        Validate.notNull(message, "Message is required for a flash message");
        Validate.notBlank(slot,
                "Slot name must be specified for a flash message");
        if (!"".equals(message)) {
            logger.log(level, message);
        }
    }

    @CliCommand(value = { "flash test" }, help = "Tests message flashing")
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

    protected abstract ExecutionStrategy getExecutionStrategy();

    public ExitShellRequest getExitShellRequest() {
        return exitShellRequest;
    }

    /**
     * Obtains the home directory for the current shell instance.
     * <p>
     * Note: calls the {@link #getHomeAsString()} method to allow subclasses to
     * provide the home directory location as string using different
     * environment-specific strategies.
     * <p>
     * If the path indicated by {@link #getHomeAsString()} exists and refers to
     * a directory, that directory is returned.
     * <p>
     * If the path indicated by {@link #getHomeAsString()} exists and refers to
     * a file, an exception is thrown.
     * <p>
     * If the path indicated by {@link #getHomeAsString()} does not exist, it
     * will be created as a directory. If this fails, an exception will be
     * thrown.
     * 
     * @return the home directory for the current shell instance (which is
     *         guaranteed to exist and be a directory)
     */
    public File getHome() {
        final String rooHome = getHomeAsString();
        final File f = new File(rooHome);
        Validate.isTrue(!f.exists() || f.exists() && f.isDirectory(), "Path '"
                + f.getAbsolutePath()
                + "' must be a directory, or it must not exist");
        if (!f.exists()) {
            f.mkdirs();
        }
        Validate.isTrue(
                f.exists() && f.isDirectory(),
                "Path '"
                        + f.getAbsolutePath()
                        + "' is not a directory; please specify roo.home system property correctly");
        return f;
    }

    protected abstract String getHomeAsString();

    protected abstract Parser getParser();

    public String getShellPrompt() {
        return shellPrompt;
    }

    @CliCommand(value = { "//", ";" }, help = "Inline comment markers (start of line only)")
    public void inlineComment() {
    }

    /**
     * Allows a subclass to log the execution of a well-formed command. This is
     * invoked after a command has completed, and indicates whether the command
     * returned normally or returned an exception. Note that attempted commands
     * that are not well-formed (eg they are missing a mandatory argument) will
     * never be presented to this method, as the command execution is never
     * actually attempted in those cases. This method is only invoked if an
     * attempt is made to execute a particular command.
     * <p>
     * Implementations should consider specially handling the "script" commands,
     * and also indicating whether a command was successful or not.
     * Implementations that wish to behave consistently with other
     * {@link AbstractShell} subclasses are encouraged to simply override
     * {@link #logCommandToOutput(String)} instead, and only override this
     * method if you actually need to fine-tune the output logic.
     * 
     * @param line the parsed line (any comments have been removed; never null)
     * @param successful if the command was successful or not
     */
    protected void logCommandIfRequired(final String line,
            final boolean successful) {
        if (line.startsWith("script")) {
            logCommandToOutput((successful ? "// " : "// [failed] ") + line);
        }
        else {
            logCommandToOutput((successful ? "" : "// [failed] ") + line);
        }
    }

    /**
     * Allows a subclass to actually write the resulting logged command to some
     * form of output. This frees subclasses from needing to implement the logic
     * within {@link #logCommandIfRequired(String, boolean)}.
     * <p>
     * Implementations should invoke {@link #getExitShellRequest()} to monitor
     * any attempts to exit the shell and release resources such as output log
     * files.
     * 
     * @param processedLine the line that should be appended to some type of
     *            output (excluding the \n character)
     */
    protected void logCommandToOutput(final String processedLine) {
    }

    /**
     * Opens the given script for reading
     * 
     * @param script the script to read (required)
     * @return a non-<code>null</code> input stream
     */
    private InputStream openScript(final File script) {
        try {
            return new BufferedInputStream(new FileInputStream(script));
        }
        catch (final FileNotFoundException fnfe) {
            // Try to find the script via the classloader
            final Collection<URL> urls = findResources(script.getName());

            // Handle search failure
            Validate.notNull(urls,
                    "Unexpected error looking for '" + script.getName() + "'");

            // Handle the search being OK but the file simply not being present
            Validate.notEmpty(urls, "Script '" + script
                    + "' not found on disk or in classpath");
            Validate.isTrue(urls.size() == 1, "More than one '" + script
                    + "' was found in the classpath; unable to continue");
            try {
                return urls.iterator().next().openStream();
            }
            catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @CliCommand(value = { "system properties" }, help = "Shows the shell's properties")
    public String props() {
        final Set<String> data = new TreeSet<String>();
        for (final Entry<Object, Object> entry : System.getProperties()
                .entrySet()) {
            data.add(entry.getKey() + " = " + entry.getValue());
        }

        return StringUtils.join(data, LINE_SEPARATOR) + LINE_SEPARATOR;
    }

    private double round(final double valueToRound,
            final int numberOfDecimalPlaces) {
        final double multiplicationFactor = Math.pow(10, numberOfDecimalPlaces);
        final double interestedInZeroDPs = valueToRound * multiplicationFactor;
        return Math.round(interestedInZeroDPs) / multiplicationFactor;
    }

    @CliCommand(value = { "script" }, help = "Parses the specified resource file and executes its commands")
    public void script(
            @CliOption(key = { "", "file" }, help = "The file to locate and execute", mandatory = true) final File script,
            @CliOption(key = "lineNumbers", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Display line numbers when executing the script") final boolean lineNumbers) {

        Validate.notNull(script, "Script file to parse is required");
        final double startedNanoseconds = System.nanoTime();

        final InputStream inputStream = openScript(script);
        try {
            int i = 0;
            for (final String line : IOUtils.readLines(inputStream)) {
                i++;
                if (lineNumbers) {
                    logger.fine("Line " + i + ": " + line);
                }
                else {
                    logger.fine(line);
                }
                if (!"".equals(line.trim())) {
                    final boolean success = executeScriptLine(line);
                    if (success
                            && (line.trim().startsWith("q") || line.trim()
                                    .startsWith("ex"))) {
                        break;
                    }
                    else if (!success) {
                        // Abort script processing, given something went wrong
                        throw new IllegalStateException(
                                "Script execution aborted");
                    }
                }
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            final double executionDurationInSeconds = (System.nanoTime() - startedNanoseconds) / 1000000000D;
            logger.fine("Script required "
                    + round(executionDurationInSeconds, 3)
                    + " seconds to execute");
        }
    }

    /**
     * Base implementation of the {@link Shell#setPromptPath(String)} method,
     * designed for simple shell implementations. Advanced implementations (eg
     * those that support ANSI codes etc) will likely want to override this
     * method and set the {@link #shellPrompt} variable directly.
     * 
     * @param path to set (can be null or empty; must NOT be formatted in any
     *            special way eg ANSI codes)
     */
    public void setPromptPath(final String path) {
        shellPrompt = (StringUtils.isNotBlank(path) ? path + " " : "")
                + ROO_PROMPT;
    }

    /**
     * Default implementation of {@link Shell#setPromptPath(String, boolean))}
     * method to satisfy STS compatibility.
     * 
     * @param path to set (can be null or empty)
     * @param overrideStyle
     */
    public void setPromptPath(final String path, final boolean overrideStyle) {
        setPromptPath(path);
    }

    public void setTailor(final Tailor tailor) {
        this.tailor = tailor;
    }

    @CliCommand(value = { "version" }, help = "Displays shell version")
    public String version(
            @CliOption(key = "", help = "Special version flags") final String extra) {
        final StringBuilder sb = new StringBuilder();

        if ("roorocks".equals(extra)) {
            sb.append("               /\\ /l").append(LINE_SEPARATOR);
            sb.append("               ((.Y(!").append(LINE_SEPARATOR);
            sb.append("                \\ |/").append(LINE_SEPARATOR);
            sb.append("                /  6~6,").append(LINE_SEPARATOR);
            sb.append("                \\ _    +-.").append(LINE_SEPARATOR);
            sb.append("                 \\`-=--^-' \\").append(LINE_SEPARATOR);
            sb.append(
                    "                  \\   \\     |\\--------------------------+")
                    .append(LINE_SEPARATOR);
            sb.append(
                    "                 _/    \\    |  Thanks for loading Roo!  |")
                    .append(LINE_SEPARATOR);
            sb.append(
                    "                (  .    Y   +---------------------------+")
                    .append(LINE_SEPARATOR);
            sb.append("               /\"\\ `---^--v---.").append(
                    LINE_SEPARATOR);
            sb.append("              / _ `---\"T~~\\/~\\/").append(
                    LINE_SEPARATOR);
            sb.append("             / \" ~\\.      !").append(LINE_SEPARATOR);
            sb.append("       _    Y      Y.~~~ /'").append(LINE_SEPARATOR);
            sb.append("      Y^|   |      | Roo 7").append(LINE_SEPARATOR);
            sb.append("      | l   |     / .   /'").append(LINE_SEPARATOR);
            sb.append("      | `L  | Y .^/   ~T").append(LINE_SEPARATOR);
            sb.append("      |  l  ! | |/  | |               ____  ____  ____")
                    .append(LINE_SEPARATOR);
            sb.append(
                    "      | .`\\/' | Y   | !              / __ \\/ __ \\/ __ \\")
                    .append(LINE_SEPARATOR);
            sb.append(
                    "      l  \"~   j l   j L______       / /_/ / / / / / / /")
                    .append(LINE_SEPARATOR);
            sb.append(
                    "       \\,____{ __\"\" ~ __ ,\\_,\\_    / _, _/ /_/ / /_/ /")
                    .append(LINE_SEPARATOR);
            sb.append("    ~~~~~~~~~~~~~~~~~~~~~~~~~~~   /_/ |_|\\____/\\____/")
                    .append(" ").append(versionInfo()).append(LINE_SEPARATOR);
            return sb.toString();
        }

        sb.append("    ____  ____  ____  ").append(LINE_SEPARATOR);
        sb.append("   / __ \\/ __ \\/ __ \\ ").append(LINE_SEPARATOR);
        sb.append("  / /_/ / / / / / / / ").append(LINE_SEPARATOR);
        sb.append(" / _, _/ /_/ / /_/ /  ").append(LINE_SEPARATOR);
        sb.append("/_/ |_|\\____/\\____/   ").append(" ").append(versionInfo())
                .append(LINE_SEPARATOR);
        sb.append(LINE_SEPARATOR);

        return sb.toString();
    }
}
