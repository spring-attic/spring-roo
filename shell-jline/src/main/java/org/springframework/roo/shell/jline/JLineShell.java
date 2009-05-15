package org.springframework.roo.shell.jline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.ANSIBuffer;
import jline.ConsoleReader;

import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.ExecutionStrategy;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.SimpleParser;
import org.springframework.roo.shell.event.AbstractShellStatusPublisher;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.logging.DeferredLogHandler;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.ExceptionUtils;

/**
 * Uses the feature-rich <a href="http://jline.sourceforge.net/">JLine</a> library to provide an interactive shell.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class JLineShell extends AbstractShellStatusPublisher implements Shell {

	private static final Logger logger = Logger.getLogger(JLineShell.class.getName());
	public static String shellPrompt = "roo> ";
    private ConsoleReader reader;
    private SimpleParser parser;
    private PrintWriter writer;
    private boolean inBlockComment = false;
    private boolean requestShutdown = false;
    private ExecutionStrategy executionStrategy;
    
	public JLineShell(ExecutionStrategy executionStrategy) {
        Assert.notNull(executionStrategy, "Execution strategy required");
        this.executionStrategy = executionStrategy;
        
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

		writer = new PrintWriter(System.out);
        logger.info(version(null));
        
        logger.info("Welcome to Spring ROO. For assistance press TAB or type \"hint\" then hit ENTER.");
        
        setShellStatus(ShellStatus.STARTED);
	}
	
    public void promptLoop() {
    	setShellStatus(ShellStatus.USER_INPUT);
    	String line;
        try {
            while (!requestShutdown && ( (line = reader.readLine() ) != null) ) {
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
    
	@CliCommand(value={"script"}, help="Parses the specified resource file and executes its commands")
	public void script(@CliOption(key={"","file"}, help="The file to locate and execute", mandatory=true) File resource,
						@CliOption(key="lineNumbers", mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="false") boolean lineNumbers) {
		Assert.notNull(resource, "Resource to parser is required");
		
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(resource);
		} catch (FileNotFoundException tryTheClassLoaderInstead) {}
		
		if (inputStream == null) {
			// Try to find the resource via the classloader
			inputStream = getClass().getResourceAsStream("/" + resource.getName());
			Assert.notNull(inputStream, "Resource '" + resource + "' not found on disk or in classpath");
		}
		
	    try {
	    	BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
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
		        	boolean success = executeCommand(line);
		        	if (!success) {
		        		// Abort script processing, given something went wrong
		        		logger.fine("Script execution aborted");
		        		return;
		        	}
	        	}
	        }
	        in.close();
	    } catch (IOException e) {}
	}
	
	public boolean executeCommand(String line) {
		// another command was attempted
    	setShellStatus(ShellStatus.PARSING);

		try {
			// We support simple block comments; ie a single pair per line. Anything else is unnecessarily complicated.
			if (!inBlockComment && line.contains("/*")) {
				blockCommentBegin();
				String lhs = line.substring(0, line.lastIndexOf("/*"));
				if (line.contains("*/")) {
					line = lhs + line.substring(line.lastIndexOf("*/")+2);
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
				line = line.substring(line.lastIndexOf("*/")+2);
			}
			if ("".equals(line.trim())) {
		    	setShellStatus(ShellStatus.EXECUTION_COMPLETE);
				return true;
			}
			ParseResult parseResult = parser.parse(line, writer);
		    if (parseResult != null) {
		    	setShellStatus(ShellStatus.EXECUTING);
		    	Object result = executionStrategy.execute(parseResult);
		    	setShellStatus(ShellStatus.EXECUTION_RESULT_PROCESSING);
		    	if (result != null) {
		    		if (result instanceof Iterable<?>) {
		    			for (Object o : (Iterable<?>)result) {
		    				logger.info(o.toString());
		    			}
		    		} else {
	    				logger.info(result.toString());
		    		}
		    	}
		    }
		} catch (RuntimeException ex) {
	    	setShellStatus(ShellStatus.EXECUTION_RESULT_PROCESSING);
			// We rely on execution strategy to log it
	    	//Throwable root = ExceptionUtils.extractRootCause(ex);
			//logger.log(Level.FINE, root.getMessage());
			return false;
		} finally {
			setShellStatus(ShellStatus.EXECUTION_COMPLETE);
		}
		return true;
	}

	@CliCommand(value={"exit", "quit"}, help="Exits the shell")
	public void requestExit() {
		requestShutdown = true;
	}

	@CliCommand(value={"//", ";"}, help="Inline comment markers")
	public void inlineComment() {}

	@CliCommand(value={"/*"}, help="Start of block comment")
	public void blockCommentBegin() {
		Assert.isTrue(!inBlockComment, "Cannot open a new block comment when one already active");
		logger.fine("Started comment");
		inBlockComment = true;
	}

	@CliCommand(value={"*/"}, help="End of block comment")
	public void blockCommentFinish() {
		Assert.isTrue(inBlockComment, "Cannot close a block comment when it has not been opened");
		logger.fine("Ended comment");
		inBlockComment = false;
	}

	@CliCommand(value={"props"}, help="Shows the shell's properties")
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

	@CliCommand(value={"date"}, help="Displays the local date and time")
	public String date() {
		return new SimpleDateFormat().format(new Date());
	}

	@CliCommand(value={"version"}, help="Displays shell version")
	public String version(@CliOption(key="") String extra) {
    	StringBuilder sb = new StringBuilder();
		
    	if ("jaime".equals(extra)) {
    		sb.append("               /\\ /l").append(System.getProperty("line.separator"));
    		sb.append("               ((.Y(!").append(System.getProperty("line.separator"));
    		sb.append("                \\ |/").append(System.getProperty("line.separator"));
    		sb.append("                /  6~6,").append(System.getProperty("line.separator"));
    		sb.append("                \\ _    +-.").append(System.getProperty("line.separator"));
    		sb.append("                 \\`-=--^-' \\").append(System.getProperty("line.separator"));
    		sb.append("                  \\   \\     |\\--------------------------+").append(System.getProperty("line.separator"));
    		sb.append("                 _/    \\    |  Thanks for loading ROO!  |").append(System.getProperty("line.separator"));
    		sb.append("                (  .    Y   +---------------------------+").append(System.getProperty("line.separator"));
    		sb.append("               /\"\\ `---^--v---.").append(System.getProperty("line.separator"));
    		sb.append("              / _ `---\"T~~\\/~\\/").append(System.getProperty("line.separator"));
    		sb.append("             / \" ~\\.      !").append(System.getProperty("line.separator"));
    		sb.append("       _    Y      Y.~~~ /'").append(System.getProperty("line.separator"));
    		sb.append("      Y^|   |      | ROO 7").append(System.getProperty("line.separator"));
    		sb.append("      | l   |     / .   /'").append(System.getProperty("line.separator"));
    		sb.append("      | `L  | Y .^/   ~T").append(System.getProperty("line.separator"));
    		sb.append("      |  l  ! | |/  | |               ____  ____  ____").append(System.getProperty("line.separator"));
    		sb.append("      | .`\\/' | Y   | !              / __ \\/ __ \\/ __ \\").append(System.getProperty("line.separator"));
    		sb.append("      l  \"~   j l   j L______       / /_/ / / / / / / /").append(System.getProperty("line.separator"));
    		sb.append("       \\,____{ __\"\" ~ __ ,\\_,\\_    / _, _/ /_/ / /_/ /").append(System.getProperty("line.separator"));
    		sb.append("    ~~~~~~~~~~~~~~~~~~~~~~~~~~~   /_/ |_|\\____/\\____/").append(System.getProperty("line.separator"));
    		return sb.toString();
    	}
    	
    	sb.append("    ____  ____  ____  ").append(System.getProperty("line.separator")); 
		sb.append("   / __ \\/ __ \\/ __ \\ ").append(System.getProperty("line.separator"));
		sb.append("  / /_/ / / / / / / / ").append(System.getProperty("line.separator"));
		sb.append(" / _, _/ /_/ / /_/ /  ").append(System.getProperty("line.separator"));
		sb.append("/_/ |_|\\____/\\____/   ").append(System.getProperty("line.separator"));
		sb.append(System.getProperty("line.separator"));
		
		return sb.toString();
	}

	public SimpleParser getParser() {
		return parser;
	}

	public String getShellPrompt() {
		return shellPrompt;
	}

}
