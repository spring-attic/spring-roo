package org.springframework.roo.project.wizard;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.springframework.roo.project.wizard.model.Entity;
import javafx.stage.Stage;
import org.springframework.roo.project.wizard.model.Field;
import org.springframework.roo.project.wizard.uml.UMLEntity;

/**
 * Controller to manage Project Wizard - Step 2
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class Step2Controller implements Initializable {
    
    @FXML
    private Button addBtn;
    @FXML
    private static Button removeEntityBtn;
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
    @FXML
    private static TreeView entitiesTree;
    @FXML
    public static Pane drawPane;
    @FXML
    public static Group drawGroup;
    
    public static TreeItem<String> rootNode;
    public static List<Entity> entitiesToAdd;
    public static List<UMLEntity> uMLEntitiesAdded;
    
    private static final double maxEntitiesPerLine = 5;
    private static final double spaceBetweenEntities = 25;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        // Initialize treeview
        initializeTreeView();
        
        // Loading values
        fillValues();
    }    
    
  
    @FXML
    private void onPressAdd(ActionEvent event){
        // Show step 2.1: Add Entity
        try{
            Parent newEntity = FXMLLoader.load(Step1Controller.class.getResource("/fxml/NewEntity.fxml"));

            Scene scene = new Scene(newEntity);
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
        // Getting selected entity
        TreeItem item = (TreeItem) entitiesTree.getSelectionModel().getSelectedItem();
        
        if(item != null){
            String selectedEntity = (String) item.getValue();
            for(Entity entity : entitiesToAdd){
                if(entity.getEntityName().equals(selectedEntity)){
                    entitiesToAdd.remove(entity);
                    break;
                }
            }
            refreshEntityTreeView();
        }
    
    }
    
    @FXML
    private void onPressNext(ActionEvent event){
        if(validateStep2()){
            // Saving generated entities on project
            populateProjectInfo();
            
        }
    }
    
    @FXML
    private void onPressBack(ActionEvent event){
        // Saving generated entities on project
        populateProjectInfo();
        // Show step 1: Configuration
        try{
            Parent step1 = FXMLLoader.load(Step1Controller.class.getResource("/fxml/Step1.fxml"));

            Scene scene = new Scene(step1);
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
    
    
    @FXML
    private void onPressFinish(ActionEvent event){
        
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    
    /**
     * Method that checks if Step 2 is valid before 
     * continue
     * 
     * @return true if all fields are correct
     */
    private boolean validateStep2(){   
        if(entitiesToAdd.isEmpty()){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("You have to add some entity to your project to continue.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Method to refresh treeview
     */
    public static void refreshEntityTreeView(){
        
        // Cleaning treeview
        rootNode.getChildren().clear();
        
        // Adding all entities to add
        for(Entity entity : entitiesToAdd){
            TreeItem<String> newEntity = new TreeItem<String>(entity.getEntityName());
            Node entityIcon = new ImageView(new Image(Step2Controller.class.getResourceAsStream("/icons/entity.png")));
            newEntity.setGraphic(entityIcon);
            
            newEntity.setExpanded(true);
            
            // Adding all fields to current entity
            for(Field field: entity.getEntityFields()){
                TreeItem<String> newField = new TreeItem<String>(field.getFieldName()
                        .concat(" - ")
                        .concat("(")
                        .concat(field.getType())
                        .concat(")"));
                Node fieldIcon = new ImageView(new Image(Step2Controller.class.getResourceAsStream("/icons/field.png")));
                newField.setGraphic(fieldIcon);
                
                newEntity.getChildren().add(newField);
            }
            
            rootNode.getChildren().add(newEntity);
        }
        
        // Always expanded
        rootNode.setExpanded(true);
        
        entitiesTree.setRoot(rootNode);
        
        // Enabling or disabling delete button
        if(!entitiesToAdd.isEmpty()){
            removeEntityBtn.setDisable(false);
        }else{
            removeEntityBtn.setDisable(true);
        }
        
        // Refresing UML
        refreshUML();
    }
    
    /**
     * Method to refresh UML view
     */
    public static void refreshUML(){
        
        // Cleaning UML view
        drawGroup.getChildren().clear();
        uMLEntitiesAdded.clear();
        
        // Adding all entities to UML view
        for(Entity entity : entitiesToAdd){         
            UMLEntity umlEntity = new UMLEntity(entity);
            uMLEntitiesAdded.add(umlEntity);
            drawGroup.getChildren().add(umlEntity);
            
        }
        
    }
    
    
    /**
     * Method that populates info about current project
     */
    public void populateProjectInfo(){
        // Adding entities to project
        MainWizard.project.setEntities(entitiesToAdd);
      
    }
    
    /**
     * Method to initialize treeview
     */
    public void initializeTreeView(){
        entitiesToAdd = new ArrayList<Entity>();
        uMLEntitiesAdded = new ArrayList<UMLEntity>();
        Node folderIcon = new ImageView(new Image(Step2Controller.class.getResourceAsStream("/icons/folder.png")));
        rootNode = new TreeItem<String>(MainWizard.project.getProjectName(), folderIcon);
        refreshEntityTreeView();
    }
    
    /**
     * Method that load project entities
     */
    public void fillValues(){
        entitiesToAdd = MainWizard.project.getEntities();
        refreshEntityTreeView();
    }
}
