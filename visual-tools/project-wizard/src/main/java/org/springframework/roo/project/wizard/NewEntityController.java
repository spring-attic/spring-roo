package org.springframework.roo.project.wizard;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.roo.project.wizard.model.Entity;
import org.springframework.roo.project.wizard.model.Field;

/**
 * Controller to manage Project Wizard - Add new Entity to the new project
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class NewEntityController implements Initializable {
    
    @FXML
    private TextField entityNameField;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button addBtn;
    @FXML
    private Button addFieldBtn;
    @FXML
    private static Button removeFieldBtn;
    @FXML
    private Label infoLabel;
    @FXML
    private static TableView fieldsTable;
    @FXML
    private TableColumn typeColumn;
    @FXML
    private TableColumn nameColumn;
    
    public static ObservableList<Field> entityFields;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        entityFields = FXCollections.observableArrayList();
        
        // Initializing fields table
        initializeFieldsTable();
    }    

    @FXML
    private void onPressAdd(ActionEvent event){
        if(validateAddEntity()){
            // Creating new entity and adding to entitiesToAdd
            Entity newEntity = new Entity(entityNameField.getText(), entityFields);
            Step2Controller.entitiesToAdd.add(newEntity);
            
            // Refresh treeview
            Step2Controller.refreshEntityTreeView();
            
             // Closing stage
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
    private void onPressAddField(ActionEvent event){
        // Show step 2.1.1: Add Field
        try{
            Parent newField = FXMLLoader.load(Step1Controller.class.getResource("/fxml/NewField.fxml"));

            Scene scene = new Scene(newField);
            scene.getStylesheets().add("/styles/Styles.css");

            Stage newStage = new Stage();
            newStage.setTitle("Spring Roo - Project Wizard");

            newStage.setResizable(false);

            newStage.setScene(scene);
            newStage.show();

        }catch(Exception e){
            System.out.println(e.getCause());
        }
    }
    
    @FXML
    private void onPressRemove(ActionEvent event){
        Field selectedField = (Field) fieldsTable.getSelectionModel().getSelectedItem();
        
        if(selectedField != null){
            entityFields.remove(selectedField);
            refreshFieldsTable();
        }
    }
    
    /**
     * Method to validate entity creation before add new entity to generated project
     * 
     * @return 
     */
    private boolean validateAddEntity(){
        if("".equals(entityNameField.getText()) || 
                entityNameField.getText().contains(" ")){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Entity Name is not valid.");
            return false;
        }
        
        
        // Checking that not exists other entity with the same name
        for(Entity existingEntity : Step2Controller.entitiesToAdd){
            if(existingEntity.getEntityName().equals(entityNameField.getText())){
                infoLabel.setTextFill(Color.RED);
                infoLabel.setText("Entity " + entityNameField.getText() + " already exists in the project.");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Method to initialize fields table
     */
    private void initializeFieldsTable(){
        // Initializing type column
        typeColumn.setCellValueFactory(
            new PropertyValueFactory<Field,String>("type")
        );
        
        // Initializing name column
        nameColumn.setCellValueFactory(
            new PropertyValueFactory<Field,String>("fieldName")
        );
    }
    
    /**
     * Method to refresh fields table
     */
    public static void refreshFieldsTable(){
        fieldsTable.setItems(entityFields);
        
        // Enabling or disabling delete button
        if(!entityFields.isEmpty()){
            removeFieldBtn.setDisable(false);
        }else{
            removeFieldBtn.setDisable(true);
        }
    }
    
}
