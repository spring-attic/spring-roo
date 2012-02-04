package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.support.logging.HandlerUtils;

public class ManagedMessageRenderer {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(ManagedMessageRenderer.class);

    private final boolean createOperation;
    private String descriptionOfChange;
    private final File file;
    private final FilenameResolver filenameResolver;
    private String hashCode;
    private boolean includeHashCode;

    public ManagedMessageRenderer(final FilenameResolver filenameResolver,
            final File file, final boolean createOperation) {
        Validate.notNull(filenameResolver, "Filename resolver required");
        Validate.notNull(file, "File to manage required");
        this.filenameResolver = filenameResolver;
        this.file = file;
        this.createOperation = createOperation;
    }

    boolean isIncludeHashCode() {
        return includeHashCode;
    }

    void logManagedMessage() {
        final StringBuilder message = new StringBuilder();
        if (hashCode != null && includeHashCode && hashCode.length() >= 7) {
            // Display only the first 6 characters, being consistent with Git
            // hash code display conventions
            message.append(hashCode.subSequence(0, 7)).append(" ");
        }
        if (createOperation) {
            message.append("Created ");
        }
        else {
            message.append("Updated ");
        }
        message.append(filenameResolver.getMeaningfulName(file));
        if (StringUtils.isNotBlank(descriptionOfChange)) {
            message.append(" [");
            message.append(descriptionOfChange);
            message.append("]");
        }
        LOGGER.fine(message.toString());
    }

    public void setDescriptionOfChange(final String message) {
        descriptionOfChange = message;
    }

    void setHashCode(final String hashCode) {
        this.hashCode = hashCode;
    }

    void setIncludeHashCode(final boolean includeHashCode) {
        this.includeHashCode = includeHashCode;
    }
}
