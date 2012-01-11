package org.springframework.roo.addon.cloud.foundry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.support.util.Base64;
import org.springframework.roo.support.util.StringUtils;

/**
 * The credentials for logging into a cloud.
 */
public class CloudCredentials {

    private static final String EMAIL_KEY = "email";
    private static final String PASSWORD_KEY = "password";
    private static final String URL_KEY = "url";

    public static CloudCredentials decode(final String encoded) {
        if (StringUtils.isBlank(encoded)) {
            throw new IllegalStateException(
                    "Stored login invalid; cannot continue");
        }
        final Map<String, String> map = new HashMap<String, String>();
        final String[] encodedFields = encoded.split(",");
        for (final String encodedField : encodedFields) {
            final String[] valuePair = encodedField.split(":");
            if (valuePair.length == 2) {
                try {
                    map.put(valuePair[0],
                            new String(Base64.decode(valuePair[1],
                                    Base64.DO_BREAK_LINES)));
                }
                catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return new CloudCredentials(map);
    }

    private final String email;
    private final String password;

    private final String url;

    /**
     * Constructor that reads the relevant entries of the given map
     * 
     * @param properties the map from which to read (required)
     */
    public CloudCredentials(final Map<String, String> properties) {
        this(properties.get(EMAIL_KEY), properties.get(PASSWORD_KEY),
                properties.get(URL_KEY));
    }

    /**
     * Constructor that accepts distinct values
     * 
     * @param email the email address with which to log in (can be blank)
     * @param password the password for that email address (can be blank)
     * @param url the URL to log into (can be blank)
     */
    public CloudCredentials(final String email, final String password,
            final String url) {
        this.email = email;
        this.password = password;
        this.url = url;
    }

    public String encode() {
        if (!isValid()) {
            throw new IllegalStateException(
                    "Credentials invalid; cannot continue");
        }
        final StringBuilder sb = new StringBuilder();
        try {
            sb.append(EMAIL_KEY)
                    .append(":")
                    .append(Base64.encodeBytes(getEmail().getBytes(),
                            Base64.DO_BREAK_LINES)).append(",");
            sb.append(PASSWORD_KEY)
                    .append(":")
                    .append(Base64.encodeBytes(getPassword().getBytes(),
                            Base64.DO_BREAK_LINES)).append(",");
            sb.append(URL_KEY)
                    .append(":")
                    .append(Base64.encodeBytes(getUrl().getBytes(),
                            Base64.DO_BREAK_LINES));
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final CloudCredentials clouldCredentials = (CloudCredentials) o;

        if (email != null ? !email.equals(clouldCredentials.email)
                : clouldCredentials.email != null) {
            return false;
        }
        if (url != null ? !url.equals(clouldCredentials.url)
                : clouldCredentials.url != null) {
            return false;
        }

        return true;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Returns the URL for these credentials
     * 
     * @return <code>null</code> if none is set
     */
    public URL getUrlObject() {
        if (StringUtils.hasText(url)) {
            try {
                return new URL(url);
            }
            catch (final MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = (31 * result) + (url != null ? url.hashCode() : 0);
        return result;
    }

    /**
     * Indicates whether the given account details match these credentials
     * 
     * @param url the URL to check (can be <code>null</code>)
     * @param email the email to check (can be <code>null</code>)
     * @return see above
     */
    public boolean isSameAccount(final String url, final String email) {
        return StringUtils.equals(url, this.url)
                && StringUtils.equals(email, this.email);
    }

    /**
     * Indicates whether these credentials are complete, i.e. contain enough
     * information to attempt a login
     * 
     * @return see above
     */
    public boolean isValid() {
        return StringUtils.hasText(email) && StringUtils.hasText(password)
                && StringUtils.hasText(url);
    }
}