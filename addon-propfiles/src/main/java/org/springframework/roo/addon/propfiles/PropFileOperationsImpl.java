package org.springframework.roo.addon.propfiles;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Provides property file configuration operations.
 *
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class PropFileOperationsImpl implements PropFileOperations {
	
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	
	public boolean isPropertiesCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	public void changeProperty(Path propertyFilePath, String propertyFilename, String key, String value) {
		changeProperty(propertyFilePath, propertyFilename, key, value, false);
	}
	
	public void changeProperty(Path propertyFilePath, String propertyFilename, String key, String value, boolean sorted) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");
		Assert.hasText(value, "Value required");
		
		String filePath = pathResolver.getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		
		Properties props;
		if (sorted) {
			props = new Properties() {
				private static final long serialVersionUID = 1L;
				
				//override the keys() method to order the keys alphabetically
				@SuppressWarnings("unchecked")
				public synchronized Enumeration keys() {
					final Object[] keys = keySet().toArray();
					return new Enumeration() {
						int i = 0;
						public boolean hasMoreElements() { 
							return i < keys.length; 
						}
						public Object nextElement() { 
							return keys[i++];
						}
					};
				}
			};
		} else {
			props = new Properties();
		}
		
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
		String propValue = props.getProperty(key);
		if (propValue == null || !propValue.equals(value)) {
			props.setProperty(key, value);
			
			try {
				props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
	}
	
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

	public Map<String, String> getProperties(Path propertyFilePath, String propertyFilename) {
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
		
		Map<String, String> result = new HashMap<String, String>();
		for (Object key : props.keySet()) {
			result.put(key.toString(), props.getProperty(key.toString()));
		}
		return Collections.unmodifiableMap(result);
	}
}
