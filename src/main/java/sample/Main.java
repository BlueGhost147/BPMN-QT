package sample;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import service.BpmnModelService;
import service.LogService;

import java.util.Optional;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    // Stage = Gesamtes Fenster
    // Scene = Das was gerade angezeigt wird
    @Override
    public void start(Stage primaryStage) throws Exception {
        //Parent root = FXMLLoader.load(getClass().getResource("../main.fxml"));
        //primaryStage.setScene(new Scene(root, 400, 400));

        primaryStage.setTitle("BPMN QT");

        SplitPane splitPane = defineLayout(primaryStage);
        primaryStage.setScene(new Scene(splitPane));
        setupLogArea(splitPane);

        primaryStage.show();

        new Thread(() -> {
            BpmnAnalyser bpmnAnalyser = new BpmnAnalyser();
            bpmnAnalyser.analyseModel("C:\\FH\\BPMN\\EinfacherProzess.bpmn");
        }).start();
    }


    public SplitPane defineLayout(Stage primaryStage) {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        AnchorPane anchorPane11 = new AnchorPane();

        splitPane.getItems().add(anchorPane11);

        return splitPane;
    }

    public void setupLogArea(SplitPane splitPane)
    {
        TextArea logArea = new TextArea("");
        logArea.setEditable(false);
        //logArea.setDisable(true);

        LogService.textArea = logArea;

        splitPane.getItems().add(logArea);
    }
}
