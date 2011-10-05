package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.util.logging.Logger;

import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

public class ManagedMessageRenderer {

	// Constants
	private static final Logger logger = HandlerUtils.getLogger(ManagedMessageRenderer.class);

	// Fields
	private final FilenameResolver filenameResolver;
	private final File file;
	private String descriptionOfChange;
	private final boolean createOperation;
	private String hashCode;
	private boolean includeHashCode;

	public void setDescriptionOfChange(final String message) {
		this.descriptionOfChange = message;
	}

	void setHashCode(final String hashCode) {
		this.hashCode = hashCode;
	}

	boolean isIncludeHashCode() {
		return includeHashCode;
	}

	void setIncludeHashCode(final boolean includeHashCode) {
		this.includeHashCode = includeHashCode;
	}

	public ManagedMessageRenderer(final FilenameResolver filenameResolver, final File file, final boolean createOperation) {
		Assert.notNull(filenameResolver, "Filename resolver required");
		Assert.notNull(file, "File to manage required");
		this.filenameResolver = filenameResolver;
		this.file = file;
		this.createOperation = createOperation;
	}

	void logManagedMessage() {
		StringBuilder message = new StringBuilder();
		if (hashCode != null && includeHashCode && hashCode.length() >= 7) {
			// Display only the first 6 characters, being consistent with Git hash code display conventions
			message.append(hashCode.subSequence(0, 7)).append(" ");
		}
		if (createOperation) {
			message.append("Created ");
		} else {
			message.append("Updated ");
		}
		message.append(filenameResolver.getMeaningfulName(file));
		if (descriptionOfChange != null && descriptionOfChange.length() > 0) {
			message.append(" [");
			message.append(descriptionOfChange);
			message.append("]");
		}
		logger.fine(message.toString());
	}
}
