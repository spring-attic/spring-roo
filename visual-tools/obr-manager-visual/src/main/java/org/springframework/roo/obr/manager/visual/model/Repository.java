package org.springframework.roo.obr.manager.visual.model;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * Model that defines Repositories
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class Repository {
    
    public final SimpleStringProperty url;
    
    public Repository(String fUrl) {
        this.url = new SimpleStringProperty(fUrl);
    }
    
    public String getUrl() {
        return url.get();
    }
    public void setUrl(String fUrl) {
        url.set(fUrl);
    }
}