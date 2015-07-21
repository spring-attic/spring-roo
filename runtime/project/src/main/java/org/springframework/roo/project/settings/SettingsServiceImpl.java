package org.springframework.roo.project.settings;

import java.io.InputStream;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Provides service to manage Spring Roo user configuration located on .roo
 * project folder.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class SettingsServiceImpl implements SettingsService {

	protected final static Logger LOGGER = HandlerUtils.getLogger(SettingsServiceImpl.class);
	public static final String USER_SETTINGS_DIR = ".roo";

	@Reference
	FileManager fileManager;

	/** {@inheritdoc } */
	@Override
	public boolean hasSettings() {
		return fileManager.exists(USER_SETTINGS_DIR);
	}

	/** {@inheritdoc } */
	@Override
	public boolean hasSettings(String sId) {
		return fileManager.exists(USER_SETTINGS_DIR.concat("/").concat(sId).concat(".yml"));
	}

	@Override
	public Object getValue(String sId, String propertyName) {
		if (!hasSettings(sId)) {
			return null;
		}
		Stack settingsStack = new Stack();
		String[] propertyItems = propertyName.split("\\.");
		// Parsing group settings file using Snake YAML
		InputStream inputStream = fileManager
				.getInputStream(USER_SETTINGS_DIR.concat("/").concat(sId).concat(".yml"));
		Yaml yaml = new Yaml();
		Map<String, Object> groupChilds = (Map<String, Object>) yaml.load(inputStream);
		// Adding node child to settingsStack
		settingsStack.push(groupChilds);
		// Checking all pathItems
		for(String propertyItem : propertyItems){
			Map<String, Object> childs = (Map<String, Object>) settingsStack.peek();
			for (Map.Entry<String, Object> child : childs.entrySet()) {
				if(child.getKey().equals(propertyItem)){
					settingsStack.push(child.getValue());
				}
			}
		}
		
		return settingsStack.peek();
	}

	@Override
	public String getStringValue(String sId, String propertyName) throws IllegalArgumentException {
		return (String) getValue(sId, propertyName);
	}

	@Override
	public Map<String, Object> getMapValue(String sId, String propertyName) throws IllegalArgumentException  {
		return (Map<String, Object>) getValue(sId, propertyName);
	}

	@Override
	public boolean isStringValue(String sId, String propertyName) {
		Object value = getValue(sId, propertyName);
		if(value instanceof String){
			return true;
		}
		return false;
	}

	@Override
	public boolean hasValue(String sId, String propertyName) {
		Object value = getValue(sId, propertyName);
		if(value != null){
			return true;
		}
		return false;
	}

}
