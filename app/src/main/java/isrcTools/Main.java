package isrcTools;
import isrcTools.TrackProviders.*;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.util.UUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader();
    String prefix = System.getProperty("user.dir");
    if (!prefix.endsWith("/app")) prefix += "/app";
    Parent root = loader.load(new FileInputStream(prefix + "/src/main/java/isrcTools/Window.fxml"));

    RootController controller = loader.getController();

    Scene scene = new Scene(root, 640, 480); 
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    /*track sample = new track.trackBuilder(conn)
      .friendlyName("Despacito")
      .isrc("USUM71607007")
      .build();*/

    launch();
  }
}

