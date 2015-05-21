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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import static org.springframework.roo.project.wizard.model.Commands.SPRING_ROO_ADDON_CREATE_ADVANCED_COMMAND;
import static org.springframework.roo.project.wizard.model.Commands.SPRING_ROO_ADDON_CREATE_SIMPLE_COMMAND;
import static org.springframework.roo.project.wizard.model.Commands.SPRING_ROO_ADDON_CREATE_SUITE_COMMAND;
import static org.springframework.roo.project.wizard.model.Commands.SPRING_ROO_PROJECT_SETUP_COMMAND;

/**
 * Controller to manage Project Wizard - Step 1
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class Step1Controller implements Initializable {
    
    @FXML
    private TextField projectNameField;
    @FXML
    private TextField packageField;
    @FXML
    private ComboBox typeCombo; 
    @FXML
    private TextField descriptionField;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Button backBtn;
    @FXML
    private Button nextBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button finishBtn;
    @FXML
    private Label infoLabel;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        // Initialize combobox with project Types
        initializeCombobox();
        
        // Loading values
        fillValues();
    }    
    
    @FXML
    private void onChangeProjectType(ActionEvent event){
        // Depens of project type, developer could execute
        // different actions
        if(typeCombo.getValue() == "Standard"){
            // Disabling description field if is an standard project
            descriptionField.setDisable(true);
            descriptionLabel.setDisable(true);
            descriptionField.setText("");
            // Enabling next button
            nextBtn.setDisable(false);
        }else{
            // Enabling description field
            descriptionField.setDisable(false);
            descriptionLabel.setDisable(false);
            descriptionField.setText("");
            // Disabling next button
            nextBtn.setDisable(true);
        }
    }
    
    @FXML
    private void onPressNext(ActionEvent event){
        if(validateStep1()){
            // Saving project info
            populateProjectInfo();
            
            // Show step 2: Entities
            try{
                Parent step2 = FXMLLoader.load(Step1Controller.class.getResource("/fxml/Step2.fxml"));
        
                Scene scene = new Scene(step2);
                scene.getStylesheets().add("/styles/Styles.css");

                Stage newStage = new Stage();
                newStage.setTitle("Spring Roo - Project Wizard");

                newStage.setResizable(false);

                newStage.setScene(scene);
                newStage.show();
                
                // Closing this view
                Stage stage = (Stage) nextBtn.getScene().getWindow();
                stage.close();
                
            }catch(Exception e){
                System.out.println(e.getCause());
            }
            
        }
    }
    
    @FXML
    private void onPressFinish(ActionEvent event){
        if(validateStep1()){
            // If finish on step 1, only needs to create a new project
            // by project type.
            
            String command = "";
            
            if(typeCombo.getValue().equals("Standard")){
                command = SPRING_ROO_PROJECT_SETUP_COMMAND;
            }else if(typeCombo.getValue().equals("Add-on simple")){
                command = SPRING_ROO_ADDON_CREATE_SIMPLE_COMMAND;
            }else if(typeCombo.getValue().equals("Add-on advanced")){
                command = SPRING_ROO_ADDON_CREATE_ADVANCED_COMMAND;
            }else if(typeCombo.getValue().equals("Roo Addon Suite")){
                command = SPRING_ROO_ADDON_CREATE_SUITE_COMMAND;
            }
            
            command = command
                    .concat(" --topLevelPackage ")
                    .concat(packageField.getText())
                    .concat(" --projectName ")
                    .concat("\"")
                    .concat(projectNameField.getText())
                    .concat("\"");
            
            // Adding description if needed
            if(descriptionField.getText().length() > 0){
                command = command.concat(" --description ")
                            .concat("\"")
                            .concat(descriptionField.getText())
                            .concat("\"");
            }
            
            // Executing command
            System.out.println(command);
            
            // Closing stage
            Stage stage = (Stage) finishBtn.getScene().getWindow();
            stage.close();
            
        }
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    
    /**
     * Method that initialize combobox with available project Types
     */
    private void initializeCombobox(){
        
        // Generating project types
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Standard");
        options.add("Add-on simple");
        options.add("Add-on advanced");
        options.add("Roo Addon Suite");
        typeCombo.setItems(options);
        
        // Selecting first project type
        typeCombo.getSelectionModel().select(0);
    }
    
    /**
     * Method that checks if Step 1 is valid before 
     * continue
     * 
     * @return true if all fields are correct
     */
    private boolean validateStep1(){
        
        if("".equals(projectNameField.getText())){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Project Name is not valid.");
            return false;
        }
        
        if("".equals(packageField.getText())){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Toplevel package is not valid.");
            return false;
        }
        
        if(packageField.getText().endsWith(".")){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("Toplevel package is not valid.");
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Method that populates info about current project
     */
    public void populateProjectInfo(){

        // Populate configuration project fields
        MainWizard.project.setProjectName(projectNameField.getText());
        MainWizard.project.setTopLevelPackage(packageField.getText());
        MainWizard.project.setProjectType((String) typeCombo.getValue());
        MainWizard.project.setDescription(descriptionField.getText());
    }
    
    /**
     * Method that fills fields with curren project values
     * to be able to show fields when press back button
     */
    public void fillValues(){
        // Showing project info
        projectNameField.setText(MainWizard.project.getProjectName());
        packageField.setText(MainWizard.project.getTopLevelPackage());
        descriptionField.setText(MainWizard.project.getDescription());
    }
}
