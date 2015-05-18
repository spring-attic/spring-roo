package org.springframework.roo.obr.manager.visual;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import static org.springframework.roo.obr.manager.visual.FXMLController.ADDON_CAPABILITY_NAME;
import static org.springframework.roo.obr.manager.visual.FXMLController.JDBC_CAPABILITY_NAME;
import static org.springframework.roo.obr.manager.visual.FXMLController.LIBRARY_CAPABILITY_NAME;
import static org.springframework.roo.obr.manager.visual.FXMLController.bundlesTable;
import static org.springframework.roo.obr.manager.visual.FXMLController.installedBundles;
import static org.springframework.roo.obr.manager.visual.FXMLController.installedSuites;
import org.springframework.roo.obr.manager.visual.model.Bundle;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_INSTALL_BUNDLE;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_REMOVE_BUNDLE;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_INSTALL_SUITE;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_REMOVE_SUITE;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * This class manages all events and components of
 * Spring Roo Repository Manager UI - Info Window
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class InfoController implements Initializable {
    
    @FXML
    Button cancelBtn;
    @FXML
    Button okBtn;
    @FXML
    TextArea messageTextArea;

    
       
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        printInfoAboutSelectedBundle();      
    } 
    
    
    @FXML
    private void onPressOk(ActionEvent event){
       Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    /**
     * Method that prints extended information about selected
     * Bundle on Main view.
     */
    private void printInfoAboutSelectedBundle() {

        // Message to show
        String messageToShow = "";
        
        messageToShow+="Presentation Name: " + FXMLController.currentSelection.getPresentationName() + "\n";
        messageToShow+="Symbolic Name: " + FXMLController.currentSelection.getSymbolicName()+ "\n";
        messageToShow+="Version: " + FXMLController.currentSelection.getVersion() + "\n";
        messageToShow+="URL: " + FXMLController.currentSelection.getUri()+ "\n";
        messageToShow+="Type: " + FXMLController.currentSelection.getType()+ "\n";
        messageToShow+="Status: " + FXMLController.currentSelection.getStatus()+ "\n";
        
        messageTextArea.setText(messageToShow);
        
    }
}
