package org.springframework.roo.addon.cloud.foundry.model;

public class CloudFile {
    private final String fileName;

    public CloudFile(final String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
