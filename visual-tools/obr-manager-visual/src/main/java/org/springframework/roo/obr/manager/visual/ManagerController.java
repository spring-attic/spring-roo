package org.springframework.roo.obr.manager.visual;

import java.net.URL;
import java.util.List;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.roo.obr.manager.visual.model.Repository;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_REMOVE_REPOSITORY_COMMAND;

/**
 * 
 * This class manages all events and components of
 * Spring Roo Repository Manager UI - Repository Preferences
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class ManagerController implements Initializable {
    
    public static ObservableList<Repository> data;
    
    @FXML
    Button cancelBtn;
    @FXML
    Button okBtn;
    @FXML
    Button addBtn;
    @FXML
    Button removeBtn;
    @FXML
    public static TableView repositoriesTable;
    @FXML
    public static TableColumn repositoryCol;

    
       
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initializing Table
        initializeTable();
        
    } 
    
    
    @FXML
    private void onPressOk(ActionEvent event){
        Stage stage = (Stage) okBtn.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void onPressAdd(ActionEvent event){
        try{
            // Creating view
            Parent root = FXMLLoader.load(FXMLController.class.getResource("/fxml/Add.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add("/styles/Styles.css");
            Stage stage = new Stage();
            stage.setTitle("Spring Roo Repository Manager");
            stage.setScene(scene);
            stage.setResizable(false);
            // Modal Dialog
            stage.showAndWait();
                                 
        }catch(Exception e){
            System.out.println(e.getLocalizedMessage());
        }
    }
    
    @FXML
    private void onPressRemove(ActionEvent event){
        // Getting selected items
        Repository selectedItem = (Repository) repositoriesTable.getSelectionModel().getSelectedItem();
        String url = selectedItem.getUrl();
        
        // Executing Spring Roo command
        System.out.println(SPRING_ROO_REMOVE_REPOSITORY_COMMAND.concat(" ").concat(url));
        
        // Removing from installedRepositories
        FXMLController.installedRepositories.remove(url);
        
        // Updating FXMLController.repositoriesCombo
        FXMLController.initializeRepositoriesCombobox("");
      
        // Updating table
        initializeTable();
        
    }

    /**
     * Method that initialize table with installed repositories
     */
    public static void initializeTable() {
        data = FXCollections.observableArrayList();
        
        // Initializing url column
        repositoryCol.setCellValueFactory(
            new PropertyValueFactory<Repository,String>("url")
        );
        
        List<String> installedRepositories = FXMLController.installedRepositories;
        for(String url : installedRepositories){
            Repository repo = new Repository(url);
            data.add(repo);
        }
        
        repositoriesTable.setItems(data);
        
    }
}
