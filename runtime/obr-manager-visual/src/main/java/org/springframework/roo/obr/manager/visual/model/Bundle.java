package org.springframework.roo.obr.manager.visual.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * Model that defines Bundles
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class Bundle {
    
    public final SimpleBooleanProperty checked;
    public final SimpleStringProperty status;
    public final SimpleStringProperty symbolicName;
    public final SimpleStringProperty presentationName;
    public final SimpleStringProperty version;
    public final SimpleStringProperty type;
    
    public Bundle(Boolean fChecked, String fStatus, String fSymbolicName, String fPresentationName, String fVersion, String fType) {
        this.checked = new SimpleBooleanProperty(fChecked);
        this.status = new SimpleStringProperty(fStatus);
        this.symbolicName = new SimpleStringProperty(fSymbolicName);
        this.presentationName = new SimpleStringProperty(fPresentationName);
        this.version = new SimpleStringProperty(fVersion);
        this.type = new SimpleStringProperty(fType);
    }
    
    public Boolean getChecked(){
        return checked.get();
    }
    
    public void setChecked(Boolean fChecked){
        checked.set(fChecked);
    }
    
    public String getStatus() {
        return status.get();
    }
    public void setStatus(String fStatus) {
        status.set(fStatus);
    }
        
    public String getSymbolicName() {
        return symbolicName.get();
    }
    public void setSymbolicName(String fSymbolicName) {
        symbolicName.set(fSymbolicName);
    }
    
    public String getPresentationName() {
        return presentationName.get();
    }
    public void setPresentationName(String fPresentationName) {
        presentationName.set(fPresentationName);
    }
    
    public String getVersion() {
        return version.get();
    }
    public void setVersion(String fVersion) {
        version.set(fVersion);
    }
    
    public String getType() {
        return type.get();
    }
    public void setType(String fType) {
        type.set(fType);
    }
    
}