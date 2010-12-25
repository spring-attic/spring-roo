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
	
	public void setDescriptionOfChange(String message) {
		this.descriptionOfChange = message;
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
