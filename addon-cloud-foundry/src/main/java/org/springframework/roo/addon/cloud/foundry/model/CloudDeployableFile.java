package org.springframework.roo.addon.cloud.foundry.model;

import org.springframework.roo.file.monitor.event.FileDetails;

public class CloudDeployableFile {
    private final FileDetails fileDetails;

    public CloudDeployableFile(final FileDetails fileDetails) {
        this.fileDetails = fileDetails;
    }

    public FileDetails getFileDetails() {
        return fileDetails;
    }

    public String getPath() {
        if (fileDetails != null) {
            return fileDetails.getCanonicalPath();
        }

        return null;
    }
}
