package org.springframework.roo.project.wizard.model;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * Class that defines an entity of generated project
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class Entity {
   
    public final StringProperty entityName;
    public final List<Field> entityFields;
    
    /**
     * Saving UML values
     */
    private double positionUMLX;
    private double positionUMLY;
    
    public Entity(String fEntityName, List<Field> fEntityFields){
        this.entityName = new SimpleStringProperty(fEntityName);
        this.entityFields = fEntityFields;
        this.positionUMLX = 0;
        this.positionUMLY = 0;
    }
    
    public String getEntityName(){
        return entityName.get();
    }
    
    public void setEntityName(String fEntityName){
        entityName.set(fEntityName);
    } 
    
    public List<Field> getEntityFields(){
        return entityFields;
    }
    
        public double getPositionUMLX(){
        return this.positionUMLX;
    }
    
    public void setPositionUMLX(double positionX){
        this.positionUMLX = positionX;
    }
    
    public double getPositionUMLY(){
        return this.positionUMLY;
    }
    
    public void setPositionUMLY(double positionY){
        this.positionUMLY = positionY;
    }
}
