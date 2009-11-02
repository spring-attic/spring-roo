package org.springframework.roo.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

@ScopeDevelopment
public class AddOnOperations {
	private Logger logger = Logger.getLogger(AddOnOperations.class.getName());
	
	private final Shell shell;
	
	public AddOnOperations(Shell shell) {
		this.shell = shell;
	}
	
	/**
	 * Returns the home directory of the Roo installation obtained from the current {@link Shell} implementation.
	 * 
	 * @return the ROO_HOME directory
	 */
	private File getRooHome() {
		return shell.getHome();
	}
	
	/**
	 * Creates the ROO_HOME/work directory. Throws an exception if the directory could not be created for any reason.
	 * 
	 * @return the work directory (which is guaranteed to exist and be a directory)
	 */
	private File getDir(String path) {
		File home = getRooHome();
		File dir = new File(home, path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		Assert.isTrue(dir.exists() && dir.isDirectory(), "Path '" + dir.getAbsolutePath() + "' does not exist as a directory");
		return dir;
	}

	/**
	 * @return true if a shell restart is desired
	 */
	public boolean cleanUp() {
		File rooHome = getRooHome();
		File addonDir = getDir("add-ons");
		File workDir = getDir("work");
		logger.fine("Roo home.....: " + rooHome.getAbsolutePath());
		logger.fine("Add-ons dir..: " + addonDir.getAbsolutePath());
		logger.fine("Work dir.....: " + workDir.getAbsolutePath());
		
		boolean changesPending = false;
		
		// Delete "work" directory contents (remove everything except *.jar, and track the *.jar for deferred deletion decision)
		Set<String> workJarsToDelete = new HashSet<String>();
		for (File candidate : workDir.listFiles()) {
			if (candidate.isDirectory()) {
				// Directories should never be under work
				logger.log(Level.WARNING, "ERROR " + candidate.getAbsolutePath() + " should not exist; removing on shutdown");
				candidate.deleteOnExit();
				changesPending = true;
				continue;
			}
			if (candidate.getName().endsWith(".jar")) {
				workJarsToDelete.add(candidate.getName());
			} else {
				// It's not a JAR file, so it shouldn't be in here
				logger.log(Level.WARNING, "ERROR " + candidate.getAbsolutePath() + " should not exist; removing on shutdown");
				candidate.deleteOnExit();
				changesPending = true;
			}
		}
		
		File[] addons = addonDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains("roo") && name.contains("addon") && name.endsWith(".zip");
			}
		});
		
		logger.fine("");
		for (File addon : addons) {
			logger.fine(">>>>> " + addon.getName());
			try {
				ZipFile zf = new ZipFile(addon);
				Enumeration<? extends ZipEntry> entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
						File tmpFile = new File(entry.getName()); // we'll never write file out using this variable, so it's OK to use CWD by default
						String simpleName = tmpFile.getName();
						String parent = tmpFile.getParentFile().getName();
						tmpFile = null; // just to clarify we'll never write this file out using this name
						if (!"dist".equals(parent) && !"lib".equals(parent)) {
							// Not a dist or lib JAR, so we don't care about this file
							continue;
						}
						
						// Don't delete this file
						if (workJarsToDelete.contains(simpleName)) {
							workJarsToDelete.remove(simpleName);
						}
						
						// Verify any existing file looks to be about the same
						File workFile = new File(workDir, simpleName);
						if (workFile.exists() && (workFile.length() != entry.getSize())) {
							logger.log(Level.WARNING, "ERROR " + simpleName + " (dir size=" + workFile.length() + "; zip size=" + entry.getSize() + ")");
							// give up, we've told the user about the problem; we can't rewrite the file as it's probably in use by this JVM
							continue;
						}

						if (workFile.exists()) {
							// to get here the work file exists and is the correct size, so we'll leave it alone
							logger.fine("EXIST " + simpleName);
							continue;
						}
						
						// To get here the file doesn't exist, so create it
						FileOutputStream out = new FileOutputStream(workFile);
						FileCopyUtils.copy(zf.getInputStream(entry), out);
						changesPending = true;
						logger.fine("SAVED " + simpleName);
					}
				}
				
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
			logger.fine("");
		}
		
		// Now we need to remove unwanted files on shutdown
		for (String unwanted : workJarsToDelete) {
			File delete = new File(workDir, unwanted);
			logger.fine("ERASE " + unwanted);
			changesPending = true;
			delete.deleteOnExit();
		}
		
		// an exit code of 100 means we want the Roo startup script to reload Roo
		if (changesPending) {
			logger.log(Level.SEVERE, "Restarting Spring Roo");
			return true;
		}
		
		return false;
	}

	public boolean install(String url) {
		Assert.hasText(url, "URL required");
		Assert.isTrue(url.endsWith(".zip"), "Add-ons URLs must end with .zip");
		
		URL u;
		try {
			u = new URL(url);
		} catch (Exception e) {
			throw new IllegalStateException("Error obtaining URL '" + url + "'");
		}

		File addonDir = getDir("add-ons");

		String path = u.getPath().replace('\\', '/'); // get rid of windows paths if present
		String filename = path.substring(path.lastIndexOf('/') + 1);
		
		try {
			File tmpFile = File.createTempFile(filename, "zip");
			logger.fine("Downloading " + u);
			
			FileCopyUtils.copy(u.openStream(), new FileOutputStream(tmpFile));
			logger.fine("Received " + tmpFile.length() + " bytes");
			
			// We have the file, but check it seems OK
			ZipFile zf = new ZipFile(tmpFile);
			Enumeration<? extends ZipEntry> entries = zf.entries();
			boolean looksOk = false;
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().contains("dist") && entry.getName().endsWith(".jar")) {
					looksOk = true;
					continue;
				}
			}
			
			Assert.isTrue(looksOk, "Downloaded file does not appear to be a Roo add-on");

			// To be here it looks close enough to a Roo add-on
			File target = new File(addonDir, filename);
			if (target.exists()) {
				target.delete();
			}
			FileCopyUtils.copy(tmpFile, target);
			logger.fine("Written to " + target.getAbsolutePath());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		logger.fine("Performing clean");
		logger.fine("");
		return cleanUp();
	}

	public boolean uninstall(String pattern) {
		Assert.hasText(pattern, "Deletion pattern required");
		File addonDir = getDir("add-ons");
		logger.fine("Add-ons dir..: " + addonDir.getAbsolutePath());
		File[] addons = addonDir.listFiles();
		if (addons.length == 0) {
			logger.fine("No add-ons installed");
			return false;
		}
		
		boolean changesPending = false;
		for (File addon : addons) {
			if (FileDetails.matchesAntPath(pattern, addon.getName())) {
				logger.log(Level.WARNING, "DELETE " + addon.getName());
				addon.delete();
				changesPending = true;
			} else {
				logger.fine("RETAIN " + addon.getName());
			}
		}
		
		if (changesPending) {
			logger.fine("Performing clean");
			logger.fine("");
			return cleanUp();
		}

		return false;
	}

	public void list() {
		File addonDir = getDir("add-ons");
		logger.fine("Add-ons dir..: " + addonDir.getAbsolutePath());
		logger.fine("");
		File[] addons = addonDir.listFiles();
		if (addons.length == 0) {
			logger.fine("No add-ons installed");
			return;
		}
		
		for (File addon : addons) {
			logger.fine(addon.getName() + " (" + addon.length() + " bytes)");
		}
	}

}
