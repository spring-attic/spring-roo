package org.springframework.roo.obr.manager.visual;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 
 * This class initialize Spring Roo Repository Manager that allows user 
 * to manage his installed addons and installed repositories with a graphical 
 * environment
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
                       
        // Creating view
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Main.fxml"));
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");
        
        stage.setTitle("Spring Roo Repository Manager");
        stage.setScene(scene);
        
        stage.setResizable(false);
        
        stage.show();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
