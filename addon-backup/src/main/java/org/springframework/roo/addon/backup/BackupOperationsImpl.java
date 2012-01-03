package org.springframework.roo.addon.backup;

import java.io.BufferedInputStream;
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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;

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

    // Constants
    private static final Logger LOGGER = HandlerUtils
            .getLogger(BackupOperationsImpl.class);

    // Fields
    @Reference private FileManager fileManager;
    @Reference private ProjectOperations projectOperations;

    public boolean isBackupPossible() {
        return projectOperations.isFocusedProjectAvailable();
    }

    public String backup() {
        Assert.isTrue(isBackupPossible(), "Project metadata unavailable");

        // For Windows, make a date format that can legally form part of a
        // filename (ROO-277)
        final String pattern = File.separatorChar == '\\' ? "yyyy-MM-dd_HH.mm.ss"
                : "yyyy-MM-dd_HH:mm:ss";
        final DateFormat df = new SimpleDateFormat(pattern);
        long start = System.nanoTime();

        ZipOutputStream zos = null;
        try {
            File projectDirectory = new File(projectOperations
                    .getPathResolver().getFocusedIdentifier(Path.ROOT, "."));
            MutableFile file = fileManager.createFile(FileUtils
                    .getCanonicalPath(new File(projectDirectory,
                            projectOperations.getFocusedProjectName() + "_"
                                    + df.format(new Date()) + ".zip")));
            zos = new ZipOutputStream(file.getOutputStream());
            zip(projectDirectory, projectDirectory, zos);
        }
        catch (FileNotFoundException e) {
            LOGGER.fine("Could not determine project directory");
        }
        catch (IOException e) {
            LOGGER.fine("Could not create backup archive");
        }
        finally {
            IOUtils.closeQuietly(zos);
        }

        long milliseconds = (System.nanoTime() - start) / 1000000;
        return "Backup completed in " + milliseconds + " ms";
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

        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                if (files[i].listFiles().length == 0) {
                    ZipEntry dirEntry = new ZipEntry(files[i].getPath()
                            .substring(base.getPath().length() + 1)
                            + File.separatorChar);
                    zos.putNextEntry(dirEntry);
                }
                zip(files[i], base, zos);
            }
            else {
                InputStream inputStream = null;
                try {
                    inputStream = new BufferedInputStream(new FileInputStream(
                            files[i]));
                    ZipEntry entry = new ZipEntry(files[i].getPath().substring(
                            base.getPath().length() + 1));
                    zos.putNextEntry(entry);
                    while ((read = inputStream.read(buffer)) != -1) {
                        zos.write(buffer, 0, read);
                    }
                }
                finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
    }
}
