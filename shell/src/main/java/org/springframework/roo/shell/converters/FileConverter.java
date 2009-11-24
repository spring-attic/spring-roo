package org.springframework.roo.shell.converters;

import java.io.File;
import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * {@link Converter} for {@link File}.
 *
 * @author Stefan Schmidt
 * @author Roman Kuzmik
 * @since 1.0
 *
 */
@ScopeDevelopment
public class FileConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new File(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData,
			String optionContext, MethodTarget target) {

		String directoryData = "";
		if (existingData != null && existingData.contains(File.separator)) {
			directoryData = existingData.substring(0, existingData.lastIndexOf(File.separator) + 1);
			existingData = existingData.substring(existingData.lastIndexOf(File.separator) + 1);
		}

		populate(completions, existingData, directoryData);

		return false;
	}

	protected void populate(List<String> completions, String existingData, String directoryData) {
		File directory = new File(directoryData.length() > 0 ? directoryData : ".");
		Assert.isTrue(directory.isDirectory());

		for (File file : directory.listFiles()) {
			if (existingData == null || existingData.length() == 0 || 
				file.getName().toLowerCase().startsWith(existingData.toLowerCase())) {

				String completion = "";
				if (directoryData.length() > 0)
					completion += directoryData;
				completion += file.getName();

				if (file.isDirectory()) {
					completions.add(completion + File.separator);
				} else {
					completions.add(completion);
				}
			}
		}
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return File.class.isAssignableFrom(requiredType);
	}

}