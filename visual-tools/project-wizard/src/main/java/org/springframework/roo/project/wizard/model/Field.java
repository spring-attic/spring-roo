package org.springframework.roo.project.wizard.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * Class that defines a Entity field of generated project
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class Field {
   
    public final StringProperty fieldName;
    public final StringProperty type;
    public Entity referencedClass;
    
    public Field(String fFieldName, String fType){
        this.fieldName = new SimpleStringProperty(fFieldName);
        this.type = new SimpleStringProperty(fType);
        this.referencedClass = null;
    }
    
    public String getFieldName(){
        return fieldName.get();
    }
    
    public void setFieldName(String fFieldName){
        fieldName.set(fFieldName);
    } 
    
    public String getType(){
        return type.get();
    }
    
    public void setType(String fType){
        type.set(fType);
    } 
    
    public Entity getReferencedClass(){
        return this.referencedClass;
    }
    
    public void setReferencedClass(Entity fReferencedClass){
        this.referencedClass = fReferencedClass;
    }
}
