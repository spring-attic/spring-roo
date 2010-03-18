package org.springframework.roo.model;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

import org.springframework.roo.support.util.StringUtils;

/**
 * {@link PropertyEditor} for {@link JavaSymbolName}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class JavaSymbolNameEditor extends PropertyEditorSupport {

	@Override
	public String getAsText() {
		JavaSymbolName obj = (JavaSymbolName) getValue();
		if (obj == null) {
			return null;
		}
		return obj.getSymbolName();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (text == null || "".equals(text)) {
			setValue(null);
		}
		// Symbol names never start with a capital
		text = StringUtils.uncapitalize(text);
		setValue(new JavaSymbolName(text));
	}

	
}
