package org.springframework.roo.addon.web.mvc.controller;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.HexUtils;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Default implementation of {@link XmlRoundTripFileManager}.
 *
 * @author James Tyrrell
 * @since 1.2.0
 */
@Component
@Service
public class DefaultXmlRoundTripFileManager implements XmlRoundTripFileManager {

	// Fields
	@Reference private FileManager fileManager;
	private HashMap<String, String> fileContentsMap = new HashMap<String, String>();
	private static MessageDigest sha = null;

	static {
		try {
			sha = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException ignored) {}
	}

	public void writeToDiskIfNecessary(String filename, Document proposed) {
		Assert.notNull(filename, "The file name is required");
		Assert.notNull(proposed, "The proposed document is required");
		Document original = null;
		if (fileManager.exists(filename)) {
			String proposedContents = XmlUtils.nodeToString(proposed);
			try {
				if (sha != null) {
					String contents = FileCopyUtils.copyToString(new File(filename)) + proposedContents;
					byte[] digest = sha.digest(contents.getBytes());
					String contentsSha = HexUtils.toHex(digest);
					String lastContents = fileContentsMap.get(filename);
					if (lastContents != null && contentsSha.equals(lastContents)) {
						return;
					}
					fileContentsMap.put(filename, contentsSha);
				}
			} catch (IOException ignored) {}
		   	original = XmlUtils.readXml(fileManager.getInputStream(filename));
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				XmlUtils.removeTextNodes(original);
				fileManager.createOrUpdateTextFileIfRequired(filename, proposedContents, false);
			}
		} else {
			String contents = XmlUtils.nodeToString(proposed);
			if (sha != null) {
				byte[] digest = sha.digest((contents + contents).getBytes());
				String contentsSha = HexUtils.toHex(digest);
				fileContentsMap.put(filename, contentsSha);
			}
			fileManager.createOrUpdateTextFileIfRequired(filename, contents, false);
		}
	}
}
