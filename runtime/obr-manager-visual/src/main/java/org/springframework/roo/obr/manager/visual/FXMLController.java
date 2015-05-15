package org.springframework.roo.obr.manager.visual;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.roo.obr.manager.visual.model.Bundle;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_INSTALL_BUNDLE;
import static org.springframework.roo.obr.manager.visual.model.Commands.SPRING_ROO_REPOSITORY_MANAGER;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * This class manages all events and components of
 * Spring Roo Repository Manager UI
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class FXMLController implements Initializable {
    
    public static List<String> installedRepositories;
    public static List<String> installedBundles;
    
    private CheckBox selectAllCheckBox;
    
    @FXML
    public static ComboBox repositoriesCombo;
    @FXML
    public static TableView bundlesTable;
    @FXML
    public static TableColumn statusCol;
    @FXML
    public static TableColumn presentationNameCol;
    @FXML
    public static TableColumn versionCol;
    @FXML
    public static TableColumn checkCol;
    @FXML
    public static Button addRepository;
    @FXML
    public static Button installBtn;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        // Getting installed Bundles
        String propertyBundles = System.getProperty("installedBundles");
        String[] bundles = propertyBundles.split(",");
        installedBundles = new ArrayList<>(Arrays.asList(bundles));
        
        // Getting installed repositories
        String propertyRepos = System.getProperty("installedRepositories");
        String[] repositories = propertyRepos.split(",");
        installedRepositories = new ArrayList<>(Arrays.asList(repositories));
        
        // Initializing combobox with installed repositories
        initializeCombobox();
        
        // Initializing Table
        initializeTable();
    } 
    
    @FXML
    private void onChangeRepository(ActionEvent event){
        String url = (String) repositoriesCombo.getValue();
        populateTableUsingURL(url);
    }
    
    @FXML
    private void onAddRepository(ActionEvent event){
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
    private void onPressInstall(ActionEvent event){
         try{
            // Creating view
            Parent root = FXMLLoader.load(FXMLController.class.getResource("/fxml/Confirmation.fxml"));
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
    
    private void populateTableUsingURL(String url) { 
        
      
        ObservableList<Bundle> data = FXCollections.observableArrayList();
        
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new URL(url).openStream());
            
            doc.getDocumentElement().normalize();
            
            NodeList resources = doc.getElementsByTagName("resource");        

            for (int i = 0; i < resources.getLength(); i++) {
 		Node nNode = resources.item(i);
 		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
 
			Element eElement = (Element) nNode;
                        
                        // Getting symbolicName
                        String symbolicName = eElement.getAttribute("symbolicname");
                        
                        // Calculating bundle status
                        String status = "Not installed";
                        
                        if(installedBundles.indexOf(symbolicName) != -1){
                            status = "Installed";
                        }
                        
                        Bundle bundle = new Bundle(false, status,
                                symbolicName,
                                eElement.getAttribute("presentationname"),
                                eElement.getAttribute("version"));
                        data.add(bundle);
		}
            }
            
        }catch(ParserConfigurationException | IOException | SAXException e){
            System.out.println(e.getMessage());
        }
            
        bundlesTable.setItems(data);
    }

    /**
     * Method to initialize Repository combobox
     */
    public static void initializeCombobox() {
                // Initializing combo
        ObservableList<String> options = FXCollections.observableArrayList();
        options.addAll(installedRepositories);
        repositoriesCombo.setItems(options);
    } 

    /**
     * Method to initialize Bundle Table
     */
    private void initializeTable() {
        
        // Initializing check column
        checkCol.setCellValueFactory(new PropertyValueFactory<Bundle, Boolean>("checked"));
        checkCol.setCellFactory(new Callback<TableColumn<Bundle, Boolean>, TableCell<Bundle, Boolean>>() {
                @Override
                public TableCell<Bundle, Boolean> call(TableColumn<Bundle, Boolean> p) {
                        final TableCell<Bundle, Boolean> cell = new TableCell<Bundle, Boolean>() {
                                @Override
                                public void updateItem(final Boolean item, boolean empty) {
                                        if (item == null)
                                                return;
                                        super.updateItem(item, empty);
                                        if (!isEmpty()) {
                                                final Bundle bundle = getTableView().getItems().get(getIndex());
                                                CheckBox checkBox = new CheckBox();
                                                checkBox.selectedProperty().bindBidirectional(bundle.checked);
                                                // checkBox.setOnAction(event);
                                                setGraphic(checkBox);                                             
                                        }
                                }
                        };
                        cell.setAlignment(Pos.CENTER);
                        return cell;
                }
        });
        
        // Initializing status column
        statusCol.setCellValueFactory(
            new PropertyValueFactory<Bundle,String>("status")
        );
        
        // Initializing presentationName column
        presentationNameCol.setCellValueFactory(
            new PropertyValueFactory<Bundle,String>("presentationName")
        );
        
        // Initializing version column
        versionCol.setCellValueFactory(
            new PropertyValueFactory<Bundle,String>("version")
        );
        
    }   
}
