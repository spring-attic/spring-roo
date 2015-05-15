package org.springframework.roo.obr.manager.visual;

import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.roo.obr.manager.visual.model.Bundle;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_ADD_REPOSITORY_COMMAND;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_INSTALL_BUNDLE;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_REMOVE_BUNDLE;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_REPOSITORY_MANAGER;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 
 * This class manages all events and components of
 * Spring Roo Repository Manager UI - Confirm Changes
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class ConfirmController implements Initializable {
    
    @FXML
    Button cancelBtn;
    @FXML
    Button okBtn;
    @FXML
    TextArea messageTextArea;

    
       
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        String messageToConfirm = "";
        
        ObservableList items = FXMLController.bundlesTable.getItems();
        Iterator it = items.iterator();
        
        while(it.hasNext()){
            Bundle bundle = (Bundle) it.next();
            // If checkbox is checked, is necessary to install it
            if(bundle.getChecked()){
                String currentStatus = bundle.getStatus();
                String action = "";
                if(currentStatus.toLowerCase().equals("not installed")){
                    action = " will be installed";
                }else{
                    action = " will be removed";
                }                
                messageToConfirm+=bundle.getPresentationName().concat(action).concat("\n");
            }
        }
        
        messageTextArea.setText(messageToConfirm);
    } 
    
    
    @FXML
    private void onPressOk(ActionEvent event){
        ObservableList items = FXMLController.bundlesTable.getItems();
        Iterator it = items.iterator();
               
        while(it.hasNext()){
            Bundle bundle = (Bundle) it.next();
            // If checkbox is checked, is necessary to install it
            if(bundle.getChecked()){
                String currentStatus = bundle.getStatus();
                String command = "";
                if(currentStatus.toLowerCase().equals("not installed")){
                    command = SPRING_ROO_INSTALL_BUNDLE;
                }else{
                    command = SPRING_ROO_REMOVE_BUNDLE;
                }                
                
                System.out.println( command.concat(" ").concat(bundle.getSymbolicName()));
            }
        }
        
        
        
        // After install/remove selected bundles, 
        // restart view to refresh installed bundles
        System.out.println(SPRING_ROO_REPOSITORY_MANAGER);
        
        // Exiting from UI
        System.exit(0);
        
    }
    
    @FXML
    private void onPressCancel(ActionEvent event){
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
