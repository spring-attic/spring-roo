package org.springframework.roo.project.wizard.uml;

import java.util.ArrayList;
import java.util.List;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.springframework.roo.project.wizard.MainWizard;
import org.springframework.roo.project.wizard.Step2Controller;
import org.springframework.roo.project.wizard.model.Entity;
import org.springframework.roo.project.wizard.model.Field;

/**
 *
 * This class defines an entity on UML
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class UMLEntity extends Group{
    
    private Entity entity;
    
    private double orgSceneX;
    private double orgSceneY;
    private double orgTranslateX;
    private double orgTranslateY;
            
    // Configuration
    public static double entityWidth = 60;
    public static double entityHeight = 70;
    private double initialXPosition;
    private double initialYPosition;
    
    List<Line> references = new ArrayList<Line>();
    
    public UMLEntity(Entity entity){
        super();
        
        this.entity = entity;
        
        Group fieldsGroup = new Group();
        
        initialXPosition = entity.getPositionUMLX();
        initialYPosition = entity.getPositionUMLY();
        
        double maxFieldWidth = 0;
        
        // Adding fields
        int spaceBetweenFields = 20;
        int numberOfFields = 0;
        for(Field field : entity.getEntityFields()){
            
            // If entity has some reference field
            // , is necessary to paint it
            System.out.println(field.getReferencedClass());
            if(field.getReferencedClass() != null){
               Entity referencedEntity = field.getReferencedClass();
               double referencedPositionX = referencedEntity.getPositionUMLX();
               double referencedPositionY = referencedEntity.getPositionUMLY();
               
               // Generating reference line
               Line reference = new Line();
               reference.setStartX(initialXPosition);
               reference.setStartY(initialYPosition);
               reference.setEndX(referencedPositionX);
               reference.setEndY(referencedPositionY);
               this.getChildren().add(reference);
               references.add(reference);
               
            }
            
            numberOfFields++;
            
            String reducedType = "";
            String[] type = field.getType().split(" - ");
            if(type.length > 1){
                reducedType = type[1];
            }else{
                reducedType = type[0];
            }
            
            Text newField = new Text(field.getFieldName()
                    .concat(" - ")
                    .concat("(")
                    .concat(reducedType)
                    .concat(")"));
            newField.setX(initialXPosition + 8);
            newField.setY(initialYPosition + 30 + (spaceBetweenFields * numberOfFields));
            newField.setStroke(Color.WHITE);
            newField.setStrokeWidth(0.1);
            
            // Adding fields to group
            fieldsGroup.getChildren().add(newField);
            
            // Getting max field width
            if(newField.getLayoutBounds().getWidth() > maxFieldWidth){
                maxFieldWidth = newField.getLayoutBounds().getWidth();
            }
            
        }
        
        // Generating title
        Text title = new Text(entity.getEntityName());
        // Getting title width
        double titleWidth = title.getLayoutBounds().getWidth();
        
        // Width of entity changes by title length or fields length
        if(titleWidth > maxFieldWidth){
            entityWidth = titleWidth + 30;
            title.setX(initialXPosition + 15);
        }else{
            entityWidth = maxFieldWidth + 30;
            title.setX(initialXPosition + ((entityWidth / 2) - (titleWidth / 2)));
        }
        
        // Height of entities changes by number of fields
        entityHeight = (numberOfFields * spaceBetweenFields) + spaceBetweenFields + 30;
        
        title.setY(initialYPosition + 15);
        title.setStroke(Color.WHITE);
        title.setStrokeWidth(0.1);
        
        
        // Creating main rectangle
        Rectangle entityBox = new Rectangle();
        entityBox.setX(initialXPosition);
        entityBox.setY(initialYPosition);
        entityBox.setWidth(entityWidth);
        entityBox.setHeight(entityHeight);
        entityBox.setStroke(Color.LIGHTBLUE);
        entityBox.setStrokeWidth(4);
        entityBox.setFill(Color.WHITE);
        
        // Generating title rectangle
        Rectangle titleBox = new Rectangle();
        titleBox.setX(initialXPosition);
        titleBox.setY(initialYPosition);
        titleBox.setHeight(20);
        titleBox.setWidth(entityWidth);
        titleBox.setStroke(Color.LIGHTBLUE);
        titleBox.setStrokeWidth(4);
        titleBox.setFill(Color.LIGHTBLUE);
        
        // Adding components to group
        this.getChildren().add(entityBox);
        this.getChildren().add(titleBox);
        this.getChildren().add(title);
        this.getChildren().add(fieldsGroup);
        
        this.setOnMousePressed(new EventHandler<MouseEvent>() {
 
            @Override
            public void handle(MouseEvent t) {
                 // Getting entity
                UMLEntity entity = ((UMLEntity)(t.getSource()));
                
                orgSceneX = t.getSceneX();
                orgSceneY = t.getSceneY();
               
                orgTranslateX = entity.getTranslateX();
                orgTranslateY = entity.getTranslateY();
            }
        });
        
        this.setOnMouseDragged(new EventHandler<MouseEvent>() {
 
            @Override
            public void handle(MouseEvent t) {
                // Getting entity
                UMLEntity entity = ((UMLEntity)(t.getSource()));
                                
                double offsetX = t.getSceneX() - orgSceneX;
                double offsetY = t.getSceneY() - orgSceneY;
                
                double newTranslateX = orgTranslateX + offsetX;
                double newTranslateY = orgTranslateY + offsetY;
                
                if(newTranslateX > initialXPosition*-1 && newTranslateX < (640 - entity.getEntityWidth()) - initialXPosition){
                    entity.setTranslateX(newTranslateX);
                    // Updating entity
                    Entity entityToAdd = Step2Controller.entitiesToAdd.get(Step2Controller.entitiesToAdd.indexOf(entity.getEntity()));
                    entityToAdd.setPositionUMLX(newTranslateX);
                }
                
                if(newTranslateY > initialYPosition*-1 && newTranslateY < (395 - entity.getEntityHeight()) - initialYPosition){
                    entity.setTranslateY(newTranslateY);
                    // Updating entity
                    Entity entityToAdd = Step2Controller.entitiesToAdd.get(Step2Controller.entitiesToAdd.indexOf(entity.getEntity()));
                    entityToAdd.setPositionUMLY(newTranslateY);
                }
                

                

                // Moving references
                if(!references.isEmpty()){
                    // Cleaning old references positions
                    for(Line reference : references){
                        
                        if(!(newTranslateX > initialXPosition*-1 && newTranslateX < (640 - entity.getEntityWidth()) - initialXPosition)){
                            if(newTranslateX > 0){
                                newTranslateX = (Step2Controller.entitiesToAdd.get(0).getPositionUMLX() + reference.getEndX());
                            }else{
                                newTranslateX = (Step2Controller.entitiesToAdd.get(0).getPositionUMLX() - reference.getEndX());
                            }
                            
                        }
                        
                        if(!(newTranslateY > initialYPosition*-1 && newTranslateY < (395 - entity.getEntityHeight()) - initialYPosition)){
                            if(newTranslateY > 0){
                                newTranslateY = Step2Controller.entitiesToAdd.get(0).getPositionUMLY() + reference.getEndY();
                            }else{
                                newTranslateY = Step2Controller.entitiesToAdd.get(0).getPositionUMLY() - reference.getEndY();
                            }
                            
                        }
                        
                        entity.getChildren().remove(reference);
                    }

                    // Generating reference line
                    Line referenceLine = new Line();
                    referenceLine.setStartX(0);
                    referenceLine.setStartY(0);
                    referenceLine.setEndX(Step2Controller.entitiesToAdd.get(0).getPositionUMLX() - newTranslateX);
                    referenceLine.setEndY(Step2Controller.entitiesToAdd.get(0).getPositionUMLY() - newTranslateY);

                    /*System.out.println("Posicion Entidad: " + Step2Controller.entitiesToAdd.get(0).getPositionUMLX());
                    System.out.println("TranslateX: " + newTranslateX + " - TranslateY: " + newTranslateY);
                    System.out.println("Movimiento: " + (Step2Controller.entitiesToAdd.get(0).getPositionUMLX() - newTranslateX) + " - " + (Step2Controller.entitiesToAdd.get(0).getPositionUMLY() - newTranslateY));
                    */
                    entity.getChildren().add(referenceLine);

                    references.add(referenceLine);
                }
                
                              
            }
        });  
                
    }
    
    
    /**
     * Method that returns entity width
     * @return 
     */
    public double getEntityWidth(){
        return this.entityWidth;
    }
    
    /**
     * Method that returns entity height
     * @return 
     */
    public double getEntityHeight(){
        return this.entityHeight;
    }
    
    public Entity getEntity(){
        return this.entity;
    }
    
    public void setEntity(Entity fEntity){
        this.entity = fEntity;
    }
 
}
