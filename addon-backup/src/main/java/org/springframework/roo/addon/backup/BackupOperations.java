package org.springframework.roo.addon.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Operations for the 'backup' add-on.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class BackupOperations {

	private MetadataService metadataService;
	private FileManager fileManager;
	
	private Logger logger = Logger.getLogger(BackupOperations.class.getName());

	public BackupOperations(MetadataService metadataService, FileManager fileManager) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(fileManager, "FileManager required");
		this.metadataService = metadataService;
		this.fileManager = fileManager;
	}

	public boolean isBackupAvailable() {
		return getPathResolver() != null;
	}

	public String backup() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
		
		if (File.separatorChar == '\\') {
			// Windows, so make a date format that can legally form part of a filename (ROO-277)
			df = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		}
		
		long start = System.nanoTime();
		try {
			File projectDirectory = new File(getPathResolver().getIdentifier(Path.ROOT, "."));
			
			MutableFile file = fileManager.createFile(FileDetails.getCanonicalPath(new File(projectMetadata.getProjectName() + "_" + df.format(new Date()) + ".zip")));
			ZipOutputStream zos = new ZipOutputStream(file.getOutputStream());
			
			zip(projectDirectory, projectDirectory, zos);
			zos.close();			
		} catch (FileNotFoundException e) {
			logger.fine("Could not determine project directory");
		} catch (IOException e) {
			logger.fine("Could not create backup archive");
		}
		long milliseconds = (System.nanoTime() - start)/1000000;
		return "Backup completed in " + milliseconds + " ms";
	}

	private void zip(File directory, final File base, ZipOutputStream zos) throws IOException {
		File[] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// don't use this directory if it's "target" under base
				if (dir.equals(base) && name.equals("target")) {
					return false;
				}
				// skip existing backup files
				if (dir.equals(base) && name.endsWith(".zip")) {
					return false;
				}
				// skip files that start with "."
				return !name.startsWith(".");
			}
		});
		byte[] buffer = new byte[8192];
		int read = 0;
		for (int i = 0, n = files.length; i < n; i++) {
			if (files[i].isDirectory()) {
				if (files[i].listFiles().length == 0) {
					ZipEntry dirEntry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1) + System.getProperty("file.separator"));
					zos.putNextEntry(dirEntry);
				}
				zip(files[i], base, zos);
			} else {
				FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}

	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
}
