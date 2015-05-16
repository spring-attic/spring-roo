package org.springframework.roo.obr.manager.visual;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_ADD_REPOSITORY_COMMAND;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 
 * This class manages all events and components of
 * Spring Roo Repository Manager UI - Add Repository
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class AddController implements Initializable {
    
    @FXML
    Button cancelBtn;
    @FXML
    Button okBtn;
    @FXML
    Button archiveBtn;
    @FXML
    TextField urlTextField;
    @FXML
    Label infoLabel;
    
    
       
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Adding default http URL
        urlTextField.setText("http://");
    } 
    
    
    @FXML
    private void onPressOk(ActionEvent event){
        // Getting URL
        String url = urlTextField.getText();
             
        // Checking URL before adding
        boolean validRepo = checkOSGiRepository(url);
        
        // If is not valid, show an error on info label
        if(!validRepo){
            infoLabel.setTextFill(Color.RED);
            infoLabel.setText("'"+url+"' is not a valid OSGi repository");
            return;
        }
        
        // Formating URL before add
        if(!url.endsWith(".xml")){
            url = url.concat("/");
        }
        
        // If is a valid OSGi repository, execute Spring Roo command
        // to install new OSGi repository
        System.out.println(SPRING_ROO_ADD_REPOSITORY_COMMAND + " " + url);
        
        // Updating FXMLController.repositoriesCombo
        FXMLController.installedRepositories.add(url);
        FXMLController.initializeRepositoriesCombobox(url);
        
        // Updating ManagerController
        ManagerController.initializeTable();
        
        // Closing Add New Repository window
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
        
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void onPressArchive(ActionEvent event){
        
        // Creating new stage
        Stage stage = new Stage();
        
        FileChooser fileChooser = new FileChooser();
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML Files" ,"*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Select your OSGi Repository XML File");
        File indexFile = fileChooser.showOpenDialog(stage);
        
        if(indexFile == null){
            return;
        }
        
        // Addin file url to Repository URL text field
        urlTextField.setText("file://".concat(indexFile.getAbsolutePath()));
        
    }

    /**
     * This method checks the OSGi Repository URL before install on 
     * Spring Roo Shell
     * 
     * @param url
     * @return 
     */
    private boolean checkOSGiRepository(String url) {
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new URL(url).openStream());
            
            doc.getDocumentElement().normalize();
            
            NodeList repositoryNode = doc.getElementsByTagName("repository");   
            
            if(repositoryNode.getLength() == 0){
                return false;
            }
            
            NodeList resourcesNodeList = doc.getElementsByTagName("resource");   
            
            if(resourcesNodeList.getLength() == 0){
                return false;
            }

            return true;
            
        }catch(Exception e){
            return false;
        }
    }
}
