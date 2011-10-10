package org.springframework.roo.addon.jsf;

import org.springframework.roo.support.util.StringUtils;

/**
 * The Internet media type or content-type of an uploaded file
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public enum UploadedFileContentType {
	ZIP("application/zip"),
	PDF("application/pdf"),
	JPG("image/jpeg"),
	GIF("image/gif"),
	PNG("image/png"),
	MP3("audio/mpeg"),
	MPEG("video/mpeg");
	
	private String contentType;
	
	private UploadedFileContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}
	
	public static String getFileExtension(String contentType) {
		for (UploadedFileContentType uploadedFileContentType : UploadedFileContentType.values()) {
			if (uploadedFileContentType.getContentType().equals(contentType)) {
				return StringUtils.toLowerCase(uploadedFileContentType.name());
			}
		}
		throw new IllegalStateException("Unknown content type '" + contentType + "'");
	}
}
