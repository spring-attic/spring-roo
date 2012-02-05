package org.springframework.roo.addon.oscommands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link OsOperations} interface.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class OsOperationsImpl implements OsOperations {

    private static class LoggingInputStream extends Thread {
        private final ProcessManager processManager;
        private final Reader reader;

        public LoggingInputStream(final InputStream inputStream,
                final ProcessManager processManager) {

            reader = new InputStreamReader(inputStream);
            this.processManager = processManager;
        }

        @Override
        public void run() {
            ActiveProcessManager.setActiveProcessManager(processManager);
            // Prevent thread name from being presented in Roo shell
            Thread.currentThread().setName("");
            try {
                for (String line : IOUtils.readLines(reader)) {
                    if (line.startsWith("[ERROR]")) {
                        LOGGER.severe(line);
                    }
                    else if (line.startsWith("[WARNING]")) {
                        LOGGER.warning(line);
                    }
                    else {
                        LOGGER.info(line);
                    }
                }
            }
            catch (final IOException e) {
                if (e.getMessage().contains("No such file or directory")
                        || e.getMessage().contains("CreateProcess error=2")) {
                    LOGGER.severe("Could not locate executable; please ensure command is in your path");
                }
            }
            finally {
                IOUtils.closeQuietly(reader);
                ActiveProcessManager.clearActiveProcessManager();
            }
        }
    }

    private static final Logger LOGGER = HandlerUtils
            .getLogger(OsOperationsImpl.class);

    @Reference private PathResolver pathResolver;
    @Reference private ProcessManager processManager;

    public void executeCommand(final String command) throws IOException {
        final File root = new File(getProjectRoot());
        Validate.isTrue(root.isDirectory() && root.exists(),
                "Project root does not currently exist as a directory ('"
                        + root.getCanonicalPath() + "')");

        Thread.currentThread().setName(""); // Prevent thread name from being
                                            // presented in Roo shell
        final Process p = Runtime.getRuntime().exec(command, null, root);
        // Ensure separate threads are used for logging, as per ROO-652
        final LoggingInputStream input = new LoggingInputStream(
                p.getInputStream(), processManager);
        final LoggingInputStream errors = new LoggingInputStream(
                p.getErrorStream(), processManager);

        p.getOutputStream().close();
        input.start();
        errors.start();

        try {
            if (p.waitFor() != 0) {
                LOGGER.warning("The command '" + command
                        + "' did not complete successfully");
            }
        }
        catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getProjectRoot() {
        return pathResolver.getRoot();
    }
}