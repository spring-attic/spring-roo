package org.springframework.roo.obr.manager.visual;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.roo.obr.manager.visual.model.Bundle;
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
    
    public static final String ADDON_CAPABILITY_NAME = "commands";
    public static final String JDBC_CAPABILITY_NAME = "jdbcdriver";
    public static final String LIBRARY_CAPABILITY_NAME = "library";
    
    public static final String ALL_CATEGORY_OPTION = "-- All --";
    
    public static final String SPRING_ROO_MARKETPLACE_URL = "http://projects.spring.io/spring-roo/marketplace/";
    
    ObservableList<Bundle> data;
    ObservableList<Bundle> filteredData;
    
    public static List<String> installedRepositories;
    public static List<String> installedBundles;
    public static List<String> installedSuites;
    
    public static Bundle currentSelection;

    @FXML
    public static ComboBox repositoriesCombo;
    @FXML
    public static ComboBox categoryCombo;
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
    @FXML
    public static TextField textFilter;
    @FXML
    public static Hyperlink marketPlaceURL;
    @FXML
    public static Hyperlink preferencesURL;
    
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
        
        // Getting installed Suites
        String propertySuites = System.getProperty("installedSuites");
        String[] suites = propertySuites.split(",");
        installedSuites = new ArrayList<>(Arrays.asList(suites));
        
        // Initializing combobox with installed repositories
        initializeRepositoriesCombobox("");
        
        // Initializing category combobox
        initializeCategoryCombobox();
        
        // Initializing text filter
        initializeFilter();
        
        // Initializing Table
        initializeTable();
    } 
    
    @FXML
    private void onChangeRepository(ActionEvent event){
        // Cleaning filter text and category filter
        textFilter.clear();
        categoryCombo.getSelectionModel().select("-- All --");
        
        String url = (String) repositoriesCombo.getValue();
        populateTableUsingURL(url);
    }
    
    @FXML
    private void onChangeCategory(ActionEvent event){
        String filterText = textFilter.getText();
        updateTableWithFilter(filterText, (String) categoryCombo.getValue());
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
        
        // If there aren't any bundles to install, close visual component
         if(!hasBundlesToInstall()){
             System.exit(0);
         }
        
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
    
    @FXML
    private void openPreferences(ActionEvent event){
        try{
            // Creating view
            Parent root = FXMLLoader.load(FXMLController.class.getResource("/fxml/Manager.fxml"));
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
    private void onClickRow(MouseEvent event){
        if(event.getClickCount() == 2){
            Bundle bundle = (Bundle) bundlesTable.getSelectionModel().getSelectedItem();
            
            // Checking that bundles is not null
            if(bundle != null){
                // Saving current selection
                currentSelection = bundle;
                
                // Opening info window
                try{
                    // Creating view
                    Parent root = FXMLLoader.load(FXMLController.class.getResource("/fxml/Info.fxml"));
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
            
        }
    }
    
    private void populateTableUsingURL(String url) { 
        
        if("".equals(url) || url == null){
            data = FXCollections.observableArrayList();
            bundlesTable.setItems(data);
            return;
        }
        
        data = FXCollections.observableArrayList();
        
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new URL(url).openStream());
            
            doc.getDocumentElement().normalize();
            
            // Getting resources
            NodeList resources = doc.getElementsByTagName("resource");        

            for (int i = 0; i < resources.getLength(); i++) {
 		Node nNode = resources.item(i);
 		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
 
                    Element eElement = (Element) nNode;

                    // Getting capabilities
                    NodeList resourceCapabilities = eElement.getElementsByTagName("capability");
                    for(int x = 0; x < resourceCapabilities.getLength(); x++){
                        Node nCapability = resourceCapabilities.item(x);

                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                            Element eCapability = (Element) nCapability;

                            String capabilityName = eCapability.getAttribute("name");

                            // Getting resource type
                            String type = "";

                            if(capabilityName.equals(ADDON_CAPABILITY_NAME)){
                                type = "Addon";
                            }else if(capabilityName.equals(JDBC_CAPABILITY_NAME)){
                                type = "JDBCDriver";
                            }else if(capabilityName.equals(LIBRARY_CAPABILITY_NAME)){
                                type = "Library";
                            }else if(eElement.getAttribute("uri").endsWith(".esa")){ // Check Roo Addon Suite
                                type = "Suite";
                            }

                            // Is is a valid type, add to list
                            if("".equals(type)){
                                continue;
                            }

                            // Getting symbolicName
                            String symbolicName = eElement.getAttribute("symbolicname");

                            // Calculating bundle status
                            String status = "Not installed";

                            if(installedSuites.indexOf(symbolicName) != -1 
                                    || installedBundles.indexOf(symbolicName) != -1){
                                status = "Installed";
                            }

                            
                            Bundle bundle = new Bundle(false, status,
                                    symbolicName,
                                    eElement.getAttribute("presentationname"),
                                    eElement.getAttribute("version"),
                                    type,
                                    eElement.getAttribute("uri"));
                            data.add(bundle);
                        }
                    }
		}
            }
            
        }catch(ParserConfigurationException | IOException | SAXException e){
            System.out.println(e.getMessage());
        }
            
        bundlesTable.setItems(data);
    }

    /**
     * Method to initialize Repositories combobox
     * 
     * @param url
     */
    public static void initializeRepositoriesCombobox(String url) {
        // Initializing combo
        ObservableList<String> options = FXCollections.observableArrayList();
        options.addAll(installedRepositories);
        repositoriesCombo.setItems(options);
        
        if("".equals(url)){
            return;
        }
        
        // Selecting repository
        repositoriesCombo.getSelectionModel().select(url);
    } 
    
     /**
     * Method to initialize Category Combobox
     */
    public static void initializeCategoryCombobox() {
        // Initializing combo
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add(ALL_CATEGORY_OPTION);
        options.add("Addon");
        options.add("Suite");
        options.add("JDBCDriver");
        options.add("Library");
        categoryCombo.setItems(options);
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

    /**
     * Method to initialize filter textField
     */
    private void initializeFilter() {
        // Listen for text changes in the filter text field
        textFilter.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    String oldValue, String newValue) {

                updateTableWithFilter(newValue, (String) categoryCombo.getValue());
            }
        });
    }
    
    
    /**
     * Method to update table with results that matches with
     * filter
     * 
     * @param filter 
     * @param category
     */
    private void updateTableWithFilter(String filter, String category) {
        
        // Checking if some repository was selected
        if(data == null || data.isEmpty()){
            return;
        }
        
        // Initialize filtered data
        if(filteredData == null){
            filteredData = FXCollections.observableArrayList();
        }
        
        // Cleaning filteredData
        filteredData.clear();
        
        bundlesTable.setItems(filteredData);
        
        for(Bundle bundle : data){
            if(match(bundle, filter, category)){
                filteredData.add(bundle);
            }
            
        }
        
        bundlesTable.setItems(filteredData);
        
    }

    /**
     * Method that checks if filter terms matchs with 
     * selected Bundle
     * 
     * @param bundle
     * @param filter
     * @param category
     * @return 
     */
    private boolean match(Bundle bundle, String filter, String category) {
        
        filter = filter.toLowerCase();
        category = category.toLowerCase();
        String allOption = ALL_CATEGORY_OPTION.toLowerCase();
        
        String symbolicName = bundle.getSymbolicName().toLowerCase();
        String version = bundle.getVersion().toLowerCase();
        String presentationName = bundle.getPresentationName().toLowerCase();
        String type = bundle.getType().toLowerCase();

        if((symbolicName.indexOf(filter) != -1 || version.indexOf(filter) != -1 
                || presentationName.indexOf(filter) != -1) && ("".equals(category) || allOption.equals(category) || category.equals(type))){
                return true;
        }
        
        return false;
        
    }

    /**
     * Method that checks if some bundle is checked on bundles table
     * 
     * @return 
     */
    private boolean hasBundlesToInstall() {
        ObservableList currentItems = bundlesTable.getItems();
        Iterator it = currentItems.iterator();
        
        while(it.hasNext()){
            Bundle bundle = (Bundle) it.next();
            if(bundle.getChecked()){
                return true;
            }
        }
        return false;
    }
}
