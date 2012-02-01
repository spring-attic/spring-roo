package org.springframework.roo.addon.cloud.foundry.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

    public CloudFile convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new CloudFile(value);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        final String appName = ConverterUtils.getOptionValue("appName",
                target.getRemainingBuffer());
        String path = ConverterUtils.getOptionValue("path",
                target.getRemainingBuffer());
        if (path != null) {
            final int index = path.lastIndexOf(/* "/" */File.separator);
            if (index > 0) {
                path = path.substring(0, index + 1);
            }
            else {
                path = null;
            }
        }
        try {
            final String file = session.getClient().getFile(appName, 1, path);
            final List<String> options = getFileOptions(file);
            for (final String option : options) {
                if (path == null) {
                    path = "";
                }
                completions.add(new Completion(path + option));
            }
        }
        catch (final Exception ignored) {
        }

        return false;
    }

    private List<String> getFileOptions(final String files) {
        final String[] lines = files.split("\n");
        final List<String> options = new ArrayList<String>();
        for (final String line : lines) {
            final int index = line.indexOf(" ");
            if (index > 0) {
                options.add(line.substring(0, index));
            }
        }
        return options;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return CloudFile.class.isAssignableFrom(requiredType);
    }
}
