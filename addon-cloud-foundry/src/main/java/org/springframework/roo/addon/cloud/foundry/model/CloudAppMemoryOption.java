package org.springframework.roo.addon.cloud.foundry.model;

public class CloudAppMemoryOption {
    private final int memoryOption;

    public CloudAppMemoryOption(final int memoryOption) {
        this.memoryOption = memoryOption;
    }

    public int getMemoryOption() {
        return memoryOption;
    }
}
