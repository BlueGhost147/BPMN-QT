package sample;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import meric.MetricResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import service.BpmnModelService;
import service.BpmnService;
import service.LogService;
import validation.BpmnRule;
import validation.ValidationResult;
import validation.XmlValidationRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main extends Application {

    private BpmnAnalyser bpmnAnalyser;

    TabPane mainTab;

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

        mainTab = createTabPane();

        SplitPane splitPane = createSplitPane();
        Tab tab = addTab(mainTab, "Log & Rules", "Log and Rules");
        tab.setClosable(false);
        tab.setContent(splitPane);

        primaryStage.setScene(new Scene(mainTab));
        LogService mainTabLogService = new LogService();
        // setupLogArea(splitPane, mainTabLogService);

        primaryStage.show();

        // Create File chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("BPMN Files", "*.bpmn")
        );

        Button button = new Button("Select BPMN file");
        button.setOnAction(e -> {
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
            if (selectedFiles != null)
                selectedFiles.forEach(selectedFile -> doBpmnAnalysis(selectedFile.toPath()));
        });
        VBox settingsBox = new VBox();
        settingsBox.getChildren().add(button);
        splitPane.getItems().add(settingsBox);

        // Initiate rule engine
        bpmnAnalyser = new BpmnAnalyser();
        VBox ruleList = createRuleList();

        // Display rules
        List<BpmnRule> rules = bpmnAnalyser.getRules();
        rules.forEach(rule -> {

            TitledPane ruleExpandable = new TitledPane();
            ruleExpandable.setText(rule.toString());
            ruleExpandable.setExpanded(false);

            ruleExpandable.setContent(createRuleTileContent(rule));

            //TextField ruleTextField = new TextField(rule.toString());
            //ruleTextField.setEditable(false);
            ruleList.getChildren().add(ruleExpandable);
        });

        splitPane.getItems().add(ruleList);

        //doBpmnAnalysisForFolder("C:\\FH\\BPMN");


    }

    private Node createRuleTileContent(final BpmnRule rule) {
        VBox ruleNode = new VBox();
        ObservableList<Node> roleNodeChildren = ruleNode.getChildren();

        final TextField textFieldName = new TextField(rule.getName());
        roleNodeChildren.add(textFieldName);

        final TextField textFieldDesc = new TextField(rule.getDescription());
        roleNodeChildren.add(textFieldDesc);

        final TextField textFieldSource = new TextField(rule.getRef());
        roleNodeChildren.add(textFieldSource);

        if(rule instanceof XmlValidationRule)
        {
            XmlValidationRule xmlValidationRule = (XmlValidationRule) rule;
            TextField textFieldXSD = new TextField(xmlValidationRule.getXsdPath());
            roleNodeChildren.add(textFieldXSD);
        }

        Button saveRule = new Button("Save");
        saveRule.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                rule.setName(textFieldName.getText());
                rule.setDescription(textFieldDesc.getText());
                rule.setRef(textFieldSource.getText());
            }
        });
        roleNodeChildren.add(saveRule);

        return ruleNode;

    }

    public void doBpmnAnalysisForFolder(String folderPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            paths.filter(Files::isRegularFile).forEach(this::doBpmnAnalysis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doBpmnAnalysis(Path filename) {
        Tab bpmntab = addTab(mainTab, filename.getFileName().toString(), filename.toString());
        SplitPane splitPaneTab = createSplitPane();

        TableView tableViewValidation = createRuleResultTable();
        splitPaneTab.getItems().add(tableViewValidation);

        TextArea logAreaTab = new TextArea("Log:\n");
        logAreaTab.setEditable(false);
        splitPaneTab.getItems().add(logAreaTab);
        bpmntab.setContent(splitPaneTab);

        LogService logService = new LogService(logAreaTab);
        new Thread(() -> {
            List<ValidationResult> results = bpmnAnalyser.analyseModelValidation(filename.toAbsolutePath().toString(), logService);

            if (results != null) {
                ObservableList<ValidationResult> resultListFX = FXCollections.observableArrayList();
                resultListFX.setAll(results);
                tableViewValidation.setItems(resultListFX);
            }


        }).start();

        new Thread(() -> {
            List<MetricResult> results = bpmnAnalyser.analyseModelMetrics(filename.toAbsolutePath().toString(), logService);

            if (results != null) {
                ObservableList<MetricResult> resultListFX = FXCollections.observableArrayList();
                resultListFX.setAll(results);
                //tableView.setItems(resultListFX);
            }


        }).start();
    }


    public SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        return splitPane;
    }

    public TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        return tabPane;
    }

    /**
     * Create a new tab and add it the the main tab view
     */
    public Tab addTab(TabPane tabPane, String text, String label) {
        Tab newTab = new Tab(text, new Label(label));
        tabPane.getTabs().add(newTab);
        return newTab;
    }

    public void setupLogArea(SplitPane splitPane, LogService logService) {
        TextArea logArea = new TextArea("");
        logArea.setEditable(false);
        //logArea.setDisable(true);

        logService.textArea = logArea;

        splitPane.getItems().add(logArea);
    }

    /**
     * Create a Result TableView for rules
     *
     * @return
     */
    private TableView createRuleResultTable() {
        TableView<ValidationResult> table = new TableView<>();
        table.setEditable(false);

        TableColumn<ValidationResult, String> nameColum = new TableColumn<>("Rule name");
        nameColum.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRule().getName()));

        TableColumn<ValidationResult, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRule().getDescription()));

        TableColumn<ValidationResult, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRule().getRef()));

        TableColumn<ValidationResult, String> resultColumn = new TableColumn<>("Result");
        resultColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isValid() + ""));

        TableColumn<ValidationResult, String> errorsColumn = new TableColumn<>("Errors");
        errorsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullErrorMsg()));

        table.getColumns().addAll(nameColum, descColumn, sourceColumn, resultColumn, errorsColumn);

        return table;
    }

    private VBox createRuleList() {
        VBox vBox = new VBox();
        return vBox;
    }
}
