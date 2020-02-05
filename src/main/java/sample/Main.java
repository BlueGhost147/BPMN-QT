package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import service.BpmnModelService;
import service.LogService;
import validation.BpmnRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        TabPane mainTab = createTabPane();

        SplitPane splitPane = createSplitPane();
        Tab tab = addTab(mainTab, "Log & Rules", "Log and Rules");
        tab.setClosable(false);
        tab.setContent(splitPane);

        primaryStage.setScene(new Scene(mainTab));
        setupLogArea(splitPane);

        primaryStage.show();

        BpmnAnalyser bpmnAnalyser = new BpmnAnalyser();
        ListView<String> list = new ListView<String>();
        ObservableList<String> items = FXCollections.observableList(bpmnAnalyser.getRules().stream().map(BpmnRule::toString).collect(Collectors.toList()));
        list.setItems(items);
        splitPane.getItems().add(list);

        try (Stream<Path> paths = Files.walk(Paths.get("C:\\FH\\BPMN"))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(filename -> {
                        Tab bpmntab = addTab(mainTab, filename.getFileName().toString(), filename.toString());
                        new Thread(() -> {

                            //bpmnAnalyser.analyseModel("C:\\FH\\BPMN\\SF Merge at EndEvent.bpmn");


                            bpmnAnalyser.analyseModel(bpmntab, filename.toAbsolutePath().toString());

                        }).start();
                    });
        }


    }


    public SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        //AnchorPane anchorPane11 = new AnchorPane();

        //splitPane.getItems().add(anchorPane11);

        return splitPane;
    }

    public TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        return tabPane;
    }

    public Tab addTab(TabPane tabPane, String text, String label) {
        Tab newTab = new Tab(text, new Label(label));
        tabPane.getTabs().add(newTab);
        return newTab;
    }

    public void setupLogArea(SplitPane splitPane) {
        TextArea logArea = new TextArea("");
        logArea.setEditable(false);
        //logArea.setDisable(true);

        LogService.textArea = logArea;

        splitPane.getItems().add(logArea);
    }
}
