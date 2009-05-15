package org.springframework.roo.addon.propfiles;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Provides property file configuration operations.
 *
 * @author Ben Alex
 * @since 1.0
 */
@ScopeDevelopment
public class PropFileOperations {
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	
	public PropFileOperations(FileManager fileManager, PathResolver pathResolver) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
	}
	
	/**
	 * Changes the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to update (required)
	 * @param value the property value to set into the property key (required)
	 */
	public void changeProperty(Path propertyFilePath, String propertyFilename, String key, String value) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");
		Assert.hasText(value, "Value required");
		
		String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(filePath)) {
				mutableFile = fileManager.updateFile(filePath);
				props.load(mutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Properties file not found");
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		props.setProperty(key, value);
		
		try {
			props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
	
	/**
	 * Removes the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to remove (required)
	 */
	public void removeProperty(Path propertyFilePath, String propertyFilename, String key) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");
		
		String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(filePath)) {
				mutableFile = fileManager.updateFile(filePath);
				props.load(mutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Properties file not found");
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		props.remove(key);
		
		try {
			props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	/**
	 * Retrieves the specified property, returning null if the property or file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param key the property key to retrieve (required)
	 * @return the property value (may return null if the property file or requested property does not exist)
	 */
	public String getProperty(Path propertyFilePath, String propertyFilename, String key) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");
		
		String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(filePath)) {
				mutableFile = fileManager.updateFile(filePath);
				props.load(mutableFile.getInputStream());
			} else {
				return null;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		return props.getProperty(key);
	}

	/**
	 * Retrieves all property keys from the specified property, throwing an exception if the file does not exist.
	 * 
	 * @param propertyFilePath the location of the property file (required)
	 * @param propertyFilename the name of the property file within the specified path (required)
	 * @param includeValues if true, appends (" = theValue") to each returned string
	 * @return the keys (may return null if the property file does not exist)
	 */
	public SortedSet<String> getPropertyKeys(Path propertyFilePath, String propertyFilename, boolean includeValues) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		
		String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(filePath)) {
				props.load(new FileInputStream(filePath));
			} else {
				throw new IllegalStateException("Properties file not found");
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		SortedSet<String> result = new TreeSet<String>();
		for (Object key : props.keySet()) {
			String info;
			if (includeValues) {
				info = key.toString() + " = " + props.getProperty(key.toString());
			} else {
				info = key.toString();
			}
			result.add(info);
		}
		return result;
	}

}
