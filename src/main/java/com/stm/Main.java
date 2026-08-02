package com.stm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("Smart Task Manager & CPU Scheduling Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // The DashboardController spins up a daemon polling thread; make sure it
    // dies with the app instead of keeping the JVM alive after the window closes.
    @Override
    public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
