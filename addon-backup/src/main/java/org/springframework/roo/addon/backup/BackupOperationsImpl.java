package org.springframework.roo.addon.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Operations for the 'backup' add-on.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class BackupOperationsImpl implements BackupOperations {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(BackupOperationsImpl.class);

    @Reference private FileManager fileManager;
    @Reference private ProjectOperations projectOperations;

    public String backup() {
        Validate.isTrue(isBackupPossible(), "Project metadata unavailable");

        // For Windows, make a date format that can legally form part of a
        // filename (ROO-277)
        final String pattern = File.separatorChar == '\\' ? "yyyy-MM-dd_HH.mm.ss"
                : "yyyy-MM-dd_HH:mm:ss";
        final DateFormat df = new SimpleDateFormat(pattern);
        final long start = System.nanoTime();

        ZipOutputStream zos = null;
        try {
            final File projectDirectory = new File(projectOperations
                    .getPathResolver().getFocusedIdentifier(Path.ROOT, "."));
            final MutableFile file = fileManager.createFile(FileUtils
                    .getCanonicalPath(new File(projectDirectory,
                            projectOperations.getFocusedProjectName() + "_"
                                    + df.format(new Date()) + ".zip")));
            zos = new ZipOutputStream(file.getOutputStream());
            zip(projectDirectory, projectDirectory, zos);
        }
        catch (final FileNotFoundException e) {
            LOGGER.fine("Could not determine project directory");
        }
        catch (final IOException e) {
            LOGGER.fine("Could not create backup archive");
        }
        finally {
            IOUtils.closeQuietly(zos);
        }

        final long milliseconds = (System.nanoTime() - start) / 1000000;
        return "Backup completed in " + milliseconds + " ms";
    }

    public boolean isBackupPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }

    private void zip(final File directory, final File base,
            final ZipOutputStream zos) throws IOException {
        final File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                // Don't use this directory if it's "target" under base
                if (dir.equals(base) && name.equals("target")) {
                    return false;
                }

                // Skip existing backup files
                if (dir.equals(base) && name.endsWith(".zip")) {
                    return false;
                }

                // Skip files that start with "."
                return !name.startsWith(".");
            }
        });

        for (final File file : files) {
            if (file.isDirectory()) {
                if (file.listFiles().length == 0) {
                    final ZipEntry dirEntry = new ZipEntry(file.getPath()
                            .substring(base.getPath().length() + 1)
                            + File.separatorChar);
                    zos.putNextEntry(dirEntry);
                }
                zip(file, base, zos);
            }
            else {
                InputStream inputStream = null;
                try {
                    final ZipEntry entry = new ZipEntry(file.getPath()
                            .substring(base.getPath().length() + 1));
                    zos.putNextEntry(entry);

                    inputStream = new FileInputStream(file);
                    IOUtils.write(IOUtils.toByteArray(inputStream), zos);
                }
                finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
    }
}
