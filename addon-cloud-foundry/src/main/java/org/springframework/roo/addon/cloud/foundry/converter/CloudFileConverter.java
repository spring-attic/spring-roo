package org.springframework.roo.addon.cloud.foundry.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudFile;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 *
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class CloudFileConverter implements Converter<CloudFile> {
	@Reference private CloudFoundrySession session;

	public CloudFile convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return new CloudFile(value);
	}

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return CloudFile.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, final String existingData, final String optionContext, final MethodTarget target) {
		final String appName = ConverterUtils.getOptionValue("appName", target.getRemainingBuffer());
		String path = ConverterUtils.getOptionValue("path", target.getRemainingBuffer());
		if (path != null) {
			int index = path.lastIndexOf(/*"/"*/File.separator);
			if (index > 0) {
				path = path.substring(0, index + 1);
			} else {
				path = null;
			}
		}
		try {
			String file = session.getClient().getFile(appName, 1, path);
			List<String> options = getFileOptions(file);
			for (String option : options) {
				if (path == null) {
					path = "";
				}
				completions.add(new Completion(path + option));
			}
		} catch (Exception ignored) {
		}

		return false;
	}

	private List<String> getFileOptions(final String files) {
		String[] lines = files.split("\n");
		List<String> options = new ArrayList<String>();
		for (String line : lines) {
			int index = line.indexOf(" ");
			if (index > 0) {
				options.add(line.substring(0, index));
			}
		}
		return options;
	}
}
