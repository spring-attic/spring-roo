package org.springframework.roo.addon.cloud.foundry.model;

public class CloudLoginEmail {
    private final String email;

    public CloudLoginEmail(final String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
