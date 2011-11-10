package org.springframework.roo.addon.propfiles;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;

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

	// Constants
	private static final boolean SORTED = true;
	private static final boolean CHANGE_EXISTING = true;

	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public boolean isPropertiesCommandAvailable() {
		return projectOperations.isFocusedProjectAvailable();
	}
	
	public void addProperties(final LogicalPath propertyFilePath, final String propertyFilename, final Map<String, String> properties, final boolean sorted, final boolean changeExisting) {
		manageProperty(propertyFilePath, propertyFilename, properties, sorted, changeExisting);
	}

	public void addPropertyIfNotExists(final LogicalPath propertyFilePath, final String propertyFilename, final String key, final String value) {
		manageProperty(propertyFilePath, propertyFilename, asMap(key, value), !SORTED, !CHANGE_EXISTING);
	}

	public void addPropertyIfNotExists(final LogicalPath propertyFilePath, final String propertyFilename, final String key, final String value, final boolean sorted) {
		manageProperty(propertyFilePath, propertyFilename, asMap(key, value), sorted, !CHANGE_EXISTING);
	}

	public void changeProperty(final LogicalPath propertyFilePath, final String propertyFilename, final String key, final String value) {
		manageProperty(propertyFilePath, propertyFilename, asMap(key, value), !SORTED, CHANGE_EXISTING);
	}

	public void changeProperty(final LogicalPath propertyFilePath, final String propertyFilename, final String key, final String value, final boolean sorted) {
		manageProperty(propertyFilePath, propertyFilename, asMap(key, value), sorted, CHANGE_EXISTING);
	}

	private void manageProperty(final LogicalPath propertyFilePath, final String propertyFilename, final Map<String, String> properties, final boolean sorted, final boolean changeExisting) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.notNull(properties, "Property map required");

		String filePath = projectOperations.getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;

		Properties props;
		if (sorted) {
			props = new Properties() {
				private static final long serialVersionUID = 1L;

				// Override the keys() method to order the keys alphabetically
				@SuppressWarnings("all")
				public synchronized Enumeration keys() {
					final Object[] keys = keySet().toArray();
					Arrays.sort(keys);
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

		if (fileManager.exists(filePath)) {
			mutableFile = fileManager.updateFile(filePath);
			loadProperties(props, mutableFile.getInputStream());
		} else {
			// Unable to find the file, so let's create it
			mutableFile = fileManager.createFile(filePath);
		}

		boolean saveNeeded = false;
		for (final Entry<String, String> entry : properties.entrySet()) {
			final String key = entry.getKey();
			final String newValue = entry.getValue();
			final String existingValue = props.getProperty(key);
			if (existingValue == null || (!existingValue.equals(newValue) && changeExisting)) {
				props.setProperty(key, newValue);
				saveNeeded = true;
			}
		}

		if (saveNeeded) {
			storeProps(props, mutableFile.getOutputStream(), "Updated at " + new Date());
		}
	}

	public void removeProperty(final LogicalPath propertyFilePath, final String propertyFilename, final String key) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");

		String filePath = projectOperations.getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		Properties props = new Properties();

		if (fileManager.exists(filePath)) {
			mutableFile = fileManager.updateFile(filePath);
			loadProperties(props, mutableFile.getInputStream());
		} else {
			throw new IllegalStateException("Properties file not found");
		}

		props.remove(key);

		storeProps(props, mutableFile.getOutputStream(), "Updated at " + new Date());
	}

	public String getProperty(final LogicalPath propertyFilePath, final String propertyFilename, final String key) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");
		Assert.hasText(key, "Key required");

		String filePath = projectOperations.getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
		MutableFile mutableFile = null;
		Properties props = new Properties();

		if (fileManager.exists(filePath)) {
			mutableFile = fileManager.updateFile(filePath);
			loadProperties(props, mutableFile.getInputStream());
		} else {
			return null;
		}

		return props.getProperty(key);
	}

	public SortedSet<String> getPropertyKeys(final LogicalPath propertyFilePath, final String propertyFilename, final boolean includeValues) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");

		String filePath = projectOperations.getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
		Properties props = new Properties();

		try {
			if (fileManager.exists(filePath)) {
				loadProperties(props, new BufferedInputStream(new FileInputStream(filePath)));
			} else {
				throw new IllegalStateException("Properties file not found");
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		SortedSet<String> result = new TreeSet<String>();
		for (Object key : props.keySet()) {
			String info = key.toString();
			if (includeValues) {
				info += " = " + props.getProperty(key.toString());
			}
			result.add(info);
		}
		return result;
	}

	public Map<String, String> getProperties(final LogicalPath propertyFilePath, final String propertyFilename) {
		Assert.notNull(propertyFilePath, "Property file path required");
		Assert.hasText(propertyFilename, "Property filename required");

		String filePath = projectOperations.getPathResolver().getIdentifier(propertyFilePath, propertyFilename);
		Properties props = new Properties();

		try {
			if (fileManager.exists(filePath)) {
				loadProperties(props, new BufferedInputStream(new FileInputStream(filePath)));
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

	public Properties loadProperties(final InputStream inputStream) {
		final Properties properties = new Properties();
		if (inputStream != null) {
			loadProperties(properties, inputStream);
		}
		return properties;
	}

	private void loadProperties(final Properties props, final InputStream inputStream) {
		try {
			props.load(inputStream);
		} catch (IOException e) {
			throw new IllegalStateException("Could not load properties", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private Map<String, String> asMap(final String key, final String value) {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(key, value);
		return properties;
	}

	private void storeProps(final Properties props, final OutputStream outputStream, final String comment) {
		Assert.notNull(outputStream, "OutputStream required");
		try {
			props.store(outputStream, comment);
		} catch (IOException e) {
			throw new IllegalStateException("Could not store properties", e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	public Properties loadProperties(final String filename, final Class<?> loadingClass) {
		return loadProperties(FileUtils.getInputStream(loadingClass, filename));
	}
}
