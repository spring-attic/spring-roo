package org.springframework.roo.project.wizard.model;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * Class where all information about the project will be
 * registered
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class Project {
   
    public final StringProperty projectName;
    public final StringProperty topLevelPackage;
    public final StringProperty projectType;
    public final StringProperty description;
    public List<Entity> entities;
    
    public Project(){
        this.projectName = new SimpleStringProperty("");
        this.topLevelPackage = new SimpleStringProperty("");
        this.projectType = new SimpleStringProperty("");
        this.description = new SimpleStringProperty("");
        this.entities = new ArrayList<Entity>();
    }
    
    public Project(String fProjectName, String fTopLevelPackage, String fProjectType, String fDescription, List<Entity> entities){
        this.projectName = new SimpleStringProperty(fProjectName);
        this.topLevelPackage = new SimpleStringProperty(fTopLevelPackage);
        this.projectType = new SimpleStringProperty(fProjectType);
        this.description = new SimpleStringProperty(fDescription);
        this.entities = entities;
    }
    
    
    public String getProjectName(){
        return projectName.get();
    }
    
    public void setProjectName(String fProjectName){
        projectName.set(fProjectName);
    }
    
    public String getTopLevelPackage(){
        return topLevelPackage.get();
    }
    
    public void setTopLevelPackage(String fTopLevelPackage){
        topLevelPackage.set(fTopLevelPackage);
    }
    
    public String getProjectType(){
        return projectType.get();
    }
    
    public void setProjectType(String fProjectType){
        projectType.set(fProjectType);
    }
    
    public String getDescription(){
        return description.get();
    }
    
    public void setDescription(String fDescription){
        description.set(fDescription);
    }
    
    public List<Entity> getEntities(){
        return this.entities;
    }
    
    public void setEntities(List<Entity> entities){
        this.entities = entities;
    }
    
}
