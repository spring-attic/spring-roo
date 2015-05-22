package org.springframework.roo.project.wizard;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.roo.project.wizard.model.Entity;
import org.springframework.roo.project.wizard.model.Field;

/**
 * Controller to manage Project Wizard - Add new Entity Field
 * to the new project
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class NewFieldController implements Initializable {
    
    @FXML
    private TextField fieldNameField;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button addBtn;
    @FXML
    private Label infoLabel;
    @FXML
    private ComboBox fieldTypeCombo;
    @FXML
    private ComboBox classCombo;
    @FXML
    private Label classLabel;
    
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize fieldTypeCombo
        initializeTypeCombo();
        
        // Initialize classCombo
        initializeClassCombo();
    }    

    @FXML
    private void onPressAdd(ActionEvent event){
        if(validateAddField()){
            // Creating new field
            Field newField = new Field(fieldNameField.getText(), (String) fieldTypeCombo.getValue());
            
            // Adding class reference if needed
            if(fieldTypeCombo.getValue().equals("reference") && classCombo.getValue() != null){
                for(Entity entity : Step2Controller.entitiesToAdd){
                    if(entity.getEntityName().equals(classCombo.getValue())){
                        newField.setReferencedClass(entity);
                        break;
                   }
                }
            }
            
            // Adding field to NewEntityController table
            NewEntityController.entityFields.add(newField);
            // Refresh entity fields table
            NewEntityController.refreshFieldsTable();
            
            // Closing new field view 
            Stage stage = (Stage) addBtn.getScene().getWindow();
            stage.close();
        }
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    
    @FXML
    private void onChangeFieldType(ActionEvent event){
        // Depending of type
        if(fieldTypeCombo.getValue().equals("reference")){
            classCombo.setVisible(true);
            classLabel.setVisible(true);
            
        }else{
            classCombo.setVisible(false);
            classLabel.setVisible(false);
            classCombo.getSelectionModel().clearSelection();
        }
    }
   
    
    /**
     * Method to validate entity field creation before add 
     * new entity field to the generated project
     * 
     * @return 
     */
    private boolean validateAddField(){
        if("".equals(fieldNameField.getText()) || 
                fieldNameField.getText().contains(" ")){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Field Name is not valid.");
            return false;
        }
        
        if(fieldTypeCombo.getValue().equals("reference") && classCombo.getValue() == null){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Reference field needs a class to reference");
            return false;
        }
        
        // Check if other field with the same name was declared in the entity
        for(Field existingField : NewEntityController.entityFields){
            if(existingField.getFieldName().equals(fieldNameField.getText())){
                infoLabel.setTextFill(Color.RED);
                infoLabel.setText("Field " + fieldNameField.getText() + " is already"
                        + "declared in this Entity.");
                return false;
            }
        }
        
        return true;
    } 
    
    /**
     * Method that initializes field type combo
     * with all available field types
     */
    private void initializeTypeCombo(){
        // Generating project types
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("boolean");
        options.add("date - java.util.Calendar");
        options.add("date - java.util.Date");
        options.add("embedded");
        options.add("enum");
        options.add("file");
        options.add("list"); 
        options.add("number - byte");
        options.add("number - double");
        options.add("number - float");
        options.add("number - int");
        options.add("number - java.lang.Byte");
        options.add("number - java.lang.Double");
        options.add("number - java.lang.Float");
        options.add("number - java.lang.Integer");
        options.add("number - java.lang.Long");
        options.add("number - java.lang.Number");
        options.add("number - java.lang.Short");
        options.add("number - java.math.BigDecimal");
        options.add("number - java.math.BigInteger");
        options.add("number - long");
        options.add("number - short");
        options.add("other");
        options.add("reference");
        options.add("set");
        options.add("string");
        fieldTypeCombo.setItems(options);
        
        // Selecting first project type
        fieldTypeCombo.getSelectionModel().select(0);
    }
    
    /**
     * Method to initialize class combo with 
     * created classes
     */
    private void initializeClassCombo(){
        
        // Generating project types
        ObservableList<String> options = FXCollections.observableArrayList();
        
        // Getting all generated classes
        for(Entity entity : Step2Controller.entitiesToAdd){
            options.add(entity.getEntityName());
        }
        
        classCombo.setItems(options);
        
        // Selecting first project type
        classCombo.getSelectionModel().select(0);
        
    }
}
