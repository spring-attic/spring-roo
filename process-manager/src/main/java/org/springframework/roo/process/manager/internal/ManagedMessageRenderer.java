package org.springframework.roo.process.manager.internal;

import java.io.File;
import java.util.logging.Logger;

import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

public class ManagedMessageRenderer {
	private static final Logger logger = HandlerUtils.getLogger(ManagedMessageRenderer.class);
	private FilenameResolver filenameResolver;
	private File file;
	private String descriptionOfChange = null;
	private boolean createOperation;
	private String hashCode = null;
	private boolean includeHashCode = false;
	
	public void setDescriptionOfChange(String message) {
		this.descriptionOfChange = message;
	}
	
	void setHashCode(String hashCode) {
		this.hashCode = hashCode;
	}
	
	boolean isIncludeHashCode() {
		return includeHashCode;
	}

	void setIncludeHashCode(boolean includeHashCode) {
		this.includeHashCode = includeHashCode;
	}

	public ManagedMessageRenderer(FilenameResolver filenameResolver, File file, boolean createOperation) {
		Assert.notNull(filenameResolver, "Filename resolver required");
		Assert.notNull(file, "File to manage required");
		this.filenameResolver = filenameResolver;
		this.file = file;
		this.createOperation = createOperation;
	}
	
	void logManagedMessage() {
		StringBuilder message = new StringBuilder();
		if (hashCode != null && includeHashCode && hashCode.length() >= 7) {
			// display only the first 6 characters, being consistent with Git hash code display conventions
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
