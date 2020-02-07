package sample;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import meric.MetricResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import service.BpmnModelService;
import service.BpmnService;
import service.BpmnXmlService;
import service.LogService;
import validation.BpmnRule;
import validation.RuleList;
import validation.ValidationResult;
import validation.BpmnXmlValidationRule;
import xmlexport.XmlRuleService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Main extends Application {

    private BpmnAnalyser bpmnAnalyser;

    // Tabpane of the application
    TabPane mainTab;

    // List where all rules are displayed
    VBox ruleList;

    FileChooser ruleFileChooser;

    Stage primaryStage;

    File lastBpmnPath = null;
    File lastRulePath = null;

    public static void main(String[] args) {
        launch(args);
    }

    // Stage = Gesamtes Fenster
    // Scene = Das was gerade angezeigt wird
    @Override
    public void start(Stage primaryStage) throws Exception {
        //Parent root = FXMLLoader.load(getClass().getResource("../main.fxml"));
        // primaryStage.setScene(new Scene(root, 400, 400));

        primaryStage.setTitle("BPMN QT");

        this.primaryStage = primaryStage;

        mainTab = createTabPane();

        SplitPane splitPane = createSplitPane();
        Tab tab = addTab(mainTab, "Log & Rules", "Rules and Settings");
        tab.setClosable(false);
        tab.setContent(splitPane);

        primaryStage.setScene(new Scene(mainTab, 1000, 600));
        LogService mainTabLogService = new LogService();
        // setupLogArea(splitPane, mainTabLogService);

        primaryStage.show();

        // Create File chooser
        FileChooser bpmnFileChooser = new FileChooser();
        bpmnFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("BPMN Files", "*.bpmn")
        );

        Button button = new Button("Select BPMN file");
        button.setOnAction(e -> {
            if (lastBpmnPath != null)
                bpmnFileChooser.setInitialDirectory(lastBpmnPath);
            List<File> selectedFiles = bpmnFileChooser.showOpenMultipleDialog(primaryStage);

            if (selectedFiles != null) {
                lastBpmnPath = selectedFiles.get(0).getParentFile();
                selectedFiles.forEach(selectedFile -> doBpmnAnalysis(selectedFile.toPath()));
            }
        });
        VBox settingsBox = new VBox();
        settingsBox.getChildren().add(button);
        splitPane.getItems().add(settingsBox);

        // Initiate rule engine
        bpmnAnalyser = new BpmnAnalyser();
        //bpmnAnalyser.createTestRules();

        ruleList = createRuleList();
        ruleFileChooser = new FileChooser();
        ruleFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("QT rule files", "*.qtrules")
        );
        refreshRuleList();

        splitPane.getItems().add(ruleList);

        //doBpmnAnalysisForFolder("C:\\FH\\BPMN");
    }


    private HBox ruleActionLayout = null;

    /**
     * Refresh the rule list on the mainTab
     */
    private void refreshRuleList() {
        ruleList.getChildren().clear();

        if (ruleActionLayout == null) {
            ruleActionLayout = new HBox();
            ruleActionLayout.setSpacing(5);
            Button loadRules = new Button("Import rules");
            loadRules.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (lastRulePath != null)
                        ruleFileChooser.setInitialDirectory(lastRulePath.getParentFile());
                    File selectedFile = ruleFileChooser.showOpenDialog(primaryStage);
                    lastRulePath = selectedFile;
                    if (selectedFile != null) {
                        bpmnAnalyser.setRules(XmlRuleService.jaxbXmlFileToObject(selectedFile.getAbsolutePath()));
                        refreshRuleList();
                    }

                }
            });
            Button saveRules = new Button("Export rules");
            saveRules.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (lastRulePath != null)
                        ruleFileChooser.setInitialDirectory(lastRulePath.getParentFile());
                    File saveFile = ruleFileChooser.showSaveDialog(primaryStage);

                    if (saveFile != null) {
                        lastRulePath = saveFile;
                        XmlRuleService.jaxbObjectToXML(bpmnAnalyser.getRules(), saveFile.getPath());
                    }
                }
            });

            Button createTestRules = new Button("Create test rules");
            createTestRules.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    bpmnAnalyser.createTestRules();
                    refreshRuleList();
                }
            });

            Button clearRules = new Button("Clear rules");
            clearRules.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    bpmnAnalyser.setRules(new RuleList());
                    refreshRuleList();
                }
            });


            ruleActionLayout.getChildren().add(loadRules);
            ruleActionLayout.getChildren().add(saveRules);
            ruleActionLayout.getChildren().add(createTestRules);
            ruleActionLayout.getChildren().add(clearRules);
        }
        ruleList.getChildren().add(ruleActionLayout);

        // Display rules
        RuleList rules = bpmnAnalyser.getRules();
        rules.getRules().forEach(rule -> {

            TitledPane ruleExpandable = new TitledPane();
            ruleExpandable.setText(rule.toString());
            ruleExpandable.setExpanded(false);

            ruleExpandable.setContent(createRuleTileContent(rule));

            //TextField ruleTextField = new TextField(rule.toString());
            //ruleTextField.setEditable(false);
            ruleList.getChildren().add(ruleExpandable);
        });
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

        if (rule instanceof BpmnXmlValidationRule) {
            BpmnXmlValidationRule xmlValidationRule = (BpmnXmlValidationRule) rule;
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

        SplitPane splitPaneTabResults = createSplitPane();
        splitPaneTabResults.setOrientation(Orientation.VERTICAL);

        TableView tableViewValidation = createRuleResultTable();
        splitPaneTabResults.getItems().add(tableViewValidation);

        TableView tableViewMetric = createMertricResultTable();
        splitPaneTabResults.getItems().add(tableViewMetric);

        splitPaneTab.getItems().add(splitPaneTabResults);

        TextArea logAreaTab = new TextArea("Log:\n");
        logAreaTab.setEditable(false);
        splitPaneTab.getItems().add(logAreaTab);
        bpmntab.setContent(splitPaneTab);

        LogService logService = new LogService(logAreaTab);
        new Thread(() -> {

            Optional<BpmnModelInstance> modelInstanceOpt = BpmnModelService.loadDiagram(filename.toAbsolutePath().toString(), logService);

            if (modelInstanceOpt.isPresent()) {
                List<ValidationResult> resultsRules = bpmnAnalyser.analyseModelValidation(modelInstanceOpt.get(), logService);

                if (resultsRules != null) {
                    ObservableList<ValidationResult> resultListFX = FXCollections.observableArrayList();
                    resultListFX.setAll(resultsRules);
                    tableViewValidation.setItems(resultListFX);
                }

                List<MetricResult> resultsMetrics = bpmnAnalyser.analyseModelMetrics(modelInstanceOpt.get(), logService);

                if (resultsMetrics != null) {
                    ObservableList<MetricResult> resultListFX = FXCollections.observableArrayList();
                    resultListFX.setAll(resultsMetrics);
                    tableViewMetric.setItems(resultListFX);
                }
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

    private TableView createMertricResultTable() {
        TableView<MetricResult> table = new TableView<>();
        table.setEditable(false);

        TableColumn<MetricResult, String> nameColum = new TableColumn<>("Metric name");
        nameColum.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getName()));

        TableColumn<MetricResult, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getDescription()));

        TableColumn<MetricResult, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getRef()));

        TableColumn<MetricResult, String> resultColumn = new TableColumn<>("Result");
        resultColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue() + ""));

        TableColumn<MetricResult, String> trendColumn = new TableColumn<>("Trend");
        trendColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getTrend().toString()));

        table.getColumns().addAll(nameColum, descColumn, sourceColumn, resultColumn, trendColumn);

        return table;
    }

    private VBox createRuleList() {
        VBox vBox = new VBox();
        return vBox;
    }
}
