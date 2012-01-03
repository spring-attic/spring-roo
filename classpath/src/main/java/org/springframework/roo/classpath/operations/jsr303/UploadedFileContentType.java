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
    ZIP("application/zip"), PDF("application/pdf"), JSON("application/json"), DOC(
            "application/msword"), XLS("application/vnd.ms-excel"), JPG(
            "image/jpeg"), GIF("image/gif"), PNG("image/png"), MP3("audio/mpeg"), MP4(
            "audio/mp4"), CSV("text/csv"), CSS("text/css"), HTML("text/html"), JAVASCRIPT(
            "text/javascript"), TXT("text/plain"), XML("text/xml"), MPEG(
            "video/mpeg");

    private String contentType;

    private UploadedFileContentType(final String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static UploadedFileContentType getFileExtension(
            final String contentType) {
        for (UploadedFileContentType uploadedFileContentType : UploadedFileContentType
                .values()) {
            if (uploadedFileContentType.getContentType().equals(contentType)) {
                return uploadedFileContentType;
            }
        }
        throw new IllegalStateException("Unknown content type '" + contentType
                + "'");
    }
}
