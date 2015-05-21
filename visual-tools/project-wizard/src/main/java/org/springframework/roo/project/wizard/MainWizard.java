package org.springframework.roo.project.wizard;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.roo.project.wizard.model.Project;


public class MainWizard extends Application {
    
    public static Project project;

    @Override
    public void start(Stage stage) throws Exception { 
        
        project = new Project();
        
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Step1.fxml"));
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");
        
        stage.setTitle("Spring Roo - Project Wizard");
        
        stage.setResizable(false);
        
        stage.setScene(scene);
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
