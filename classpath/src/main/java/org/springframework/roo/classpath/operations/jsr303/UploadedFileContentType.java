package org.springframework.roo.classpath.operations.jsr303;

/**
 * The Internet media type or content-type of an uploaded file.
 * <p>
 * Only common content types are included.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public enum UploadedFileContentType {
    CSS("text/css"), CSV("text/csv"), DOC("application/msword"), GIF(
            "image/gif"), HTML("text/html"), JAVASCRIPT("text/javascript"), JPG(
            "image/jpeg"), JSON("application/json"), MP3("audio/mpeg"), MP4(
            "audio/mp4"), MPEG("video/mpeg"), PDF("application/pdf"), PNG(
            "image/png"), TXT("text/plain"), XLS("application/vnd.ms-excel"), XML(
            "text/xml"), ZIP("application/zip");

    public static UploadedFileContentType getFileExtension(
            final String contentType) {
        for (final UploadedFileContentType uploadedFileContentType : UploadedFileContentType
                .values()) {
            if (uploadedFileContentType.getContentType().equals(contentType)) {
                return uploadedFileContentType;
            }
        }
        throw new IllegalStateException("Unknown content type '" + contentType
                + "'");
    }

    private String contentType;

    private UploadedFileContentType(final String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
