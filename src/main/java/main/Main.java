package main;

import enums.Operators;
import enums.RuleOperator;
import enums.SequenceRuleType;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import meric.BpmnMetric;
import meric.MetricResult;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnModelService;
import service.BpmnXmlService;
import service.LogService;
import validation.*;
import service.XmlRuleImportExportService;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main extends Application {

    // Bpmn analyse instance
    private BpmnRuleEngine bpmnRuleEngine;

    // Tabpane of the application
    private TabPane mainTab;

    // List where all rules are displayed
    private VBox ruleList;

    // File chooser for rule import / export
    private FileChooser ruleFileChooser;

    // Primary Window of the application
    private Stage primaryStage;

    // Store the path of the file chooser for convenience
    private File lastBpmnPath = null;
    private File lastRulePath = null;

    // Rule tabPane which is currently expanded ->
    // undo expanding if another pane gets expanded
    private TitledPane curentRuleExpandable;
    private BpmnRule curentExpandedRule;

    // Rule list
    private HBox ruleActionLayout = null;
    private HBox newRuleLayout = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Prepare window
        primaryStage.setTitle("BPMN QT");
        this.primaryStage = primaryStage;
        mainTab = createTabPane();

        // Create primary tab for settings
        SplitPane splitPane = createSplitPane();
        Tab tab = addTab(mainTab, "Log & Rules", "Rules and Settings");
        tab.setClosable(false);
        tab.setContent(splitPane);

        // Show window
        primaryStage.setScene(new Scene(mainTab, 1000, 600));
        primaryStage.show();

        // Create File chooser for BPMN Files
        FileChooser bpmnFileChooser = new FileChooser();
        bpmnFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("BPMN Files", "*.bpmn")
        );

        // Create File chooser for Rules Files
        ruleFileChooser = new FileChooser();
        ruleFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("QT rule files", "*.qtrules")
        );

        // Prepare load BPMN Model Button
        Button button = createButton("Load and validate BPMN");
        button.setPadding(new Insets(5, 5, 5, 5));
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
        bpmnRuleEngine = new BpmnRuleEngine();

        // Setup rule list
        ruleList = new VBox();
        refreshRuleList();

        ScrollPane scrollRuleList = new ScrollPane();
        scrollRuleList.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRuleList.setContent(ruleList);
        splitPane.getItems().add(scrollRuleList);
    }

    /**
     * Refresh the rule list on the mainTab
     */
    private void refreshRuleList() {
        ruleList.getChildren().clear();

        if (ruleActionLayout == null) {
            ruleActionLayout = new HBox();
            ruleActionLayout.setSpacing(5);

            Button loadRules = createButton("Import rules");
            loadRules.setOnAction(event -> {
                if (lastRulePath != null)
                    ruleFileChooser.setInitialDirectory(lastRulePath.getParentFile());
                File selectedFile = ruleFileChooser.showOpenDialog(primaryStage);
                lastRulePath = selectedFile;
                if (selectedFile != null) {
                    bpmnRuleEngine.setRules(XmlRuleImportExportService.loadRulesFromXML(selectedFile.getAbsolutePath()));
                    refreshRuleList();
                }

            });
            Button saveRules = createButton("Export rules");
            saveRules.setOnAction(event -> {
                if (lastRulePath != null)
                    ruleFileChooser.setInitialDirectory(lastRulePath.getParentFile());
                File saveFile = ruleFileChooser.showSaveDialog(primaryStage);

                if (saveFile != null) {
                    lastRulePath = saveFile;
                    XmlRuleImportExportService.saveRulesToXML(bpmnRuleEngine.getRules(), saveFile.getPath());
                }
            });

            Button createTestRules = createButton("Create test rules");
            createTestRules.setOnAction(event -> {
                bpmnRuleEngine.createTestRules();
                refreshRuleList();
            });

            Button clearRules = createButton("Clear rules");
            clearRules.setOnAction(event -> {
                bpmnRuleEngine.setRules(new RuleList());
                refreshRuleList();
            });


            ruleActionLayout.getChildren().add(loadRules);
            ruleActionLayout.getChildren().add(saveRules);
            ruleActionLayout.getChildren().add(createTestRules);
            ruleActionLayout.getChildren().add(clearRules);

            newRuleLayout = new HBox();

            // Create new rule choice box and add rules types
            ChoiceBox<Class<? extends BpmnRule>> ruleTypesChoise = new ChoiceBox<>();
            ruleTypesChoise.getItems().add(BpmnElementCountRule.class);
            ruleTypesChoise.getItems().add(BpmnGatewayMergeRule.class);
            ruleTypesChoise.getItems().add(BpmnXmlValidationRule.class);
            ruleTypesChoise.getItems().add(BpmnSoundnessRule.class);
            ruleTypesChoise.getItems().add(BpmnPoolProcessRule.class);
            ruleTypesChoise.getItems().add(BpmnComplexRule.class);
            ruleTypesChoise.getItems().add(BpmnMetricRule.class);
            ruleTypesChoise.getItems().add(BpmnFlowSequenceRule.class);

            // Set default value
            ruleTypesChoise.setValue(BpmnElementCountRule.class);

            Button addNewRule = createButton("Add new rules");
            addNewRule.setOnAction(event -> {

                BpmnRule newRule = null;
                if (ruleTypesChoise.getValue() == BpmnElementCountRule.class)
                    newRule = new BpmnElementCountRule("New count rule", "", "", 0, Operators.equal, Activity.class);
                if (ruleTypesChoise.getValue() == BpmnGatewayMergeRule.class)
                    newRule = new BpmnGatewayMergeRule("New gateway rule", "", "");
                if (ruleTypesChoise.getValue() == BpmnXmlValidationRule.class)
                    newRule = new BpmnXmlValidationRule("New xml validation rule", "", "", "https://www.omg.org/spec/BPMN/20100501/BPMN20.xsd");
                if (ruleTypesChoise.getValue() == BpmnSoundnessRule.class)
                    newRule = new BpmnSoundnessRule("New soundness rule", "", "");
                if (ruleTypesChoise.getValue() == BpmnPoolProcessRule.class)
                    newRule = new BpmnPoolProcessRule("New valid pool rule", "", "");
                if (ruleTypesChoise.getValue() == BpmnComplexRule.class)
                    newRule = new BpmnComplexRule("New complex rule", "", "", RuleOperator.AND);
                if (ruleTypesChoise.getValue() == BpmnMetricRule.class)
                    newRule = new BpmnMetricRule("New metric rule", "", "", 0, Operators.equal, null);
                if (ruleTypesChoise.getValue() == BpmnFlowSequenceRule.class)
                    newRule = new BpmnFlowSequenceRule("New flow sequence rule", "", "", ExclusiveGateway.class, StartEvent.class, SequenceRuleType.NOT_ALLOWED_PREDECESSOR);

                if (newRule != null) {
                    bpmnRuleEngine.getRules().getRules().add(newRule);
                    refreshRuleList();
                }

            });

            newRuleLayout.getChildren().add(ruleTypesChoise);
            newRuleLayout.getChildren().add(addNewRule);
        }

        ruleList.getChildren().add(ruleActionLayout);
        ruleList.getChildren().add(newRuleLayout);

        // Display rules
        RuleList rules = bpmnRuleEngine.getRules();
        rules.getRules().forEach(rule -> {
            TitledPane ruleExpandable = new TitledPane();
            ruleExpandable.setText(rule.getName());

            if(curentExpandedRule == null || !curentExpandedRule.equals(rule))
                ruleExpandable.setExpanded(false);
            else {
                ruleExpandable.setExpanded(true);
                curentRuleExpandable = ruleExpandable;
            }



            ruleExpandable.setStyle(rule.isActive() ? "" : "-fx-color: #ffab91");
            //if (!rule.isActive()) ruleExpandable.setStyle("-fx-strikethrough: true;");

            // Only one rule pane should be expanded at the same time
            ruleExpandable.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    if (curentRuleExpandable != null) {
                        curentRuleExpandable.setExpanded(false);
                    }
                    curentRuleExpandable = ruleExpandable;
                    curentExpandedRule = rule;
                } else {
                    if (curentRuleExpandable == ruleExpandable) {
                        curentRuleExpandable = null;
                        curentExpandedRule = null;
                    }
                }
            });

            ruleExpandable.setContent(createRuleTileContent(rule, ruleExpandable));

            ruleList.getChildren().add(ruleExpandable);
        });
    }

    /**
     * Create a tile to edit a single rule
     */
    private Node createRuleTileContent(final BpmnRule rule, TitledPane rulePane) {
        GridPane ruleNode = new GridPane();


        ruleNode.setPadding(new Insets(5, 5, 5, 5));
        // ObservableList<Node> roleNodeChildren = ruleNode.getChildren();

        int rowC = 0;

        final Label ruleType = new Label(rule.getClass().getSimpleName());
        ruleType.setStyle("-fx-font-weight: bold");
        ruleNode.add(ruleType, 1, rowC++);

        final Label labelName = new Label("Name");
        labelName.setMinWidth(100);
        final TextField textFieldName = new TextField(rule.getName());
        textFieldName.setMinWidth(300);
        textFieldName.textProperty().addListener((observable, oldValue, newValue) -> {
            rule.setName(newValue);
            rulePane.setText(newValue);
        });
        ruleNode.add(labelName, 0, rowC);
        ruleNode.add(textFieldName, 1, rowC++);

        final Label labelDesc = new Label("Description");
        final TextField textFieldDesc = new TextField(rule.getDescription());
        textFieldDesc.textProperty().addListener((observable, oldValue, newValue) -> rule.setDescription(newValue));
        ruleNode.add(labelDesc, 0, rowC);
        ruleNode.add(textFieldDesc, 1, rowC++);

        final Label labelSource = new Label("Source");
        final TextField textFieldSource = new TextField(rule.getRef());
        textFieldSource.textProperty().addListener((observable, oldValue, newValue) -> rule.setRef(newValue));
        ruleNode.add(labelSource, 0, rowC);
        ruleNode.add(textFieldSource, 1, rowC++);

        if (rule instanceof BpmnXmlValidationRule) {
            final BpmnXmlValidationRule xmlValidationRule = (BpmnXmlValidationRule) rule;

            final Label labelXSDPath = new Label("XSD Path");
            TextField textFieldXSD = new TextField(xmlValidationRule.getXsdPath());
            textFieldXSD.textProperty().addListener((observable, oldValue, newValue) -> xmlValidationRule.setXsdPath(newValue));
            ruleNode.add(labelXSDPath, 0, rowC);
            ruleNode.add(textFieldXSD, 1, rowC++);
        } /*else if (rule instanceof BpmnOntologyValidationRule) {
            final BpmnOntologyValidationRule ontologyValidationRule = (BpmnOntologyValidationRule) rule;

            final Label labelOntologyPath = new Label("Ontology Path");
            TextField textFieldOntology = new TextField(ontologyValidationRule.getOntologyPath());
            textFieldOntology.textProperty().addListener((observable, oldValue, newValue) -> ontologyValidationRule.setOntologyPath(newValue));
            ruleNode.add(labelOntologyPath, 0, rowC);
            ruleNode.add(textFieldOntology, 1, rowC++);
        }*/ else if (rule instanceof BpmnFlowSequenceRule) {
            final BpmnFlowSequenceRule bpmnFlowSequenceRule = (BpmnFlowSequenceRule) rule;

            final Label labelElement = new Label("Element Type");
            final ChoiceBox<Class<? extends FlowNode>> choiseBoxElement = createFlowNodeChoiceBox();
            choiseBoxElement.setValue(bpmnFlowSequenceRule.getElementClass());
            choiseBoxElement.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnFlowSequenceRule.setElementClass(newValue));

            final Label labelElementPrev = new Label("Previous Type");
            final ChoiceBox<Class<? extends FlowNode>> choiseBoxElementPrev = createFlowNodeChoiceBox();
            choiseBoxElementPrev.setValue(bpmnFlowSequenceRule.getPreviousElementClass());
            choiseBoxElementPrev.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnFlowSequenceRule.setElementClass(newValue));

            final Label labelOperator = new Label("Rule type");
            final ChoiceBox<SequenceRuleType> choiseBoxOperator = new ChoiceBox<>();
            choiseBoxOperator.getItems().setAll(SequenceRuleType.values());
            choiseBoxOperator.setValue(bpmnFlowSequenceRule.getSequenceRuleType());
            choiseBoxOperator.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnFlowSequenceRule.setSequenceRuleType(newValue));
            ruleNode.add(labelOperator, 0, rowC);
            ruleNode.add(choiseBoxOperator, 1, rowC++);

            ruleNode.add(labelElement, 0, rowC);
            ruleNode.add(choiseBoxElement, 1, rowC++);

            ruleNode.add(labelElementPrev, 0, rowC);
            ruleNode.add(choiseBoxElementPrev, 1, rowC++);

        } else if (rule instanceof BpmnComplexRule) {
            final BpmnComplexRule complexRule = (BpmnComplexRule) rule;

            for (BpmnRule ruleCR : complexRule.getBpmnRuleList()) {
                Label ruleLabel = new Label(ruleCR.toString());
                ruleLabel.setPadding(new Insets(5,5,5,5));

                Button rmRule = createButton("x");
                rmRule.setMinWidth(20);
                rmRule.setOnAction(event -> {
                    complexRule.getBpmnRuleList().remove(ruleCR);
                    refreshRuleList();
                });

                HBox hBoxDisplayCRule = new HBox();
                hBoxDisplayCRule.getChildren().add(rmRule);
                hBoxDisplayCRule.getChildren().add(ruleLabel);

                ruleNode.add(hBoxDisplayCRule, 1, rowC++);

            }

            final ChoiceBox<BpmnRule> choiseRules = new ChoiceBox<>();
            final Button addRuleToComplex = createButton("+");
            addRuleToComplex.setMinWidth(20);

            ObservableList<BpmnRule> ruleListFX = FXCollections.observableArrayList();
            ruleListFX.setAll(bpmnRuleEngine.getRules().getRules());
            choiseRules.setItems(ruleListFX);

            HBox hBoxAddRule = new HBox();
            hBoxAddRule.getChildren().add(addRuleToComplex);
            hBoxAddRule.getChildren().add(choiseRules);
            ruleNode.add(hBoxAddRule, 1, rowC++);

            addRuleToComplex.setOnAction(event -> {
                complexRule.addBpmnRule(choiseRules.getValue());
                refreshRuleList();
            });

            final Label labelOperator = new Label("Operator");
            final ChoiceBox<RuleOperator> choiseBoxOperator = new ChoiceBox<>();
            choiseBoxOperator.getItems().setAll(RuleOperator.values());
            choiseBoxOperator.setValue(complexRule.getOperator());
            choiseBoxOperator.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> complexRule.setOperator(newValue));
            ruleNode.add(labelOperator, 0, rowC);
            ruleNode.add(choiseBoxOperator, 1, rowC++);
        } else if (rule instanceof BpmnMetricRule) {
            final BpmnMetricRule bpmnMetricRule = (BpmnMetricRule) rule;

            final Label labelCount = new Label("Amount");
            TextField textFieldCount = new TextField(bpmnMetricRule.getAmount() + "");
            textFieldCount.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals("")) {
                    textFieldCount.setText("0");
                    bpmnMetricRule.setAmount(0);
                } else {
                    try {
                        int newValInt = Integer.parseInt(newValue);
                        bpmnMetricRule.setAmount(newValInt);
                    } catch (NumberFormatException e) {
                        try {
                            int oldValueInt = Integer.parseInt(oldValue);
                            textFieldCount.setText(oldValueInt+"");
                            bpmnMetricRule.setAmount(oldValueInt);
                        } catch (NumberFormatException e2) {
                            textFieldCount.setText("0");
                            bpmnMetricRule.setAmount(0);
                        }
                    }
                }
            });
            ruleNode.add(labelCount, 0, rowC);
            ruleNode.add(textFieldCount, 1, rowC++);

            final Label labelOperator = new Label("Operator");
            final ChoiceBox<Operators> choiseBoxOperator = new ChoiceBox<>();
            choiseBoxOperator.getItems().setAll(Operators.values());
            choiseBoxOperator.setValue(bpmnMetricRule.getOperator());
            choiseBoxOperator.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnMetricRule.setOperator(newValue));
            ruleNode.add(labelOperator, 0, rowC);
            ruleNode.add(choiseBoxOperator, 1, rowC++);

            final Label labelElement = new Label("Bpmn metric");
            final ChoiceBox<BpmnMetric> choiseBoxMetric = new ChoiceBox<>();
            ObservableList<BpmnMetric> metricListFX = FXCollections.observableArrayList();
            metricListFX.setAll(bpmnRuleEngine.getMetrics());
            choiseBoxMetric.setItems(metricListFX);
            choiseBoxMetric.setValue(bpmnMetricRule.getMetric());
            choiseBoxMetric.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnMetricRule.setMetric(newValue));

            ruleNode.add(labelElement, 0, rowC);
            ruleNode.add(choiseBoxMetric, 1, rowC++);


        } else if (rule instanceof BpmnElementCountRule) {
            final BpmnElementCountRule bpmnElementCountRule = (BpmnElementCountRule) rule;

            final Label labelCount = new Label("Count");
            TextField textFieldCount = new TextField(bpmnElementCountRule.getCount() + "");
            textFieldCount.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals("")) {
                    textFieldCount.setText("0");
                    bpmnElementCountRule.setCount(0);
                } else {
                    try {
                        int newValInt = Integer.parseInt(newValue);
                        bpmnElementCountRule.setCount(newValInt);
                    } catch (NumberFormatException e) {
                        textFieldCount.setText(oldValue);
                    }
                }
            });
            ruleNode.add(labelCount, 0, rowC);
            ruleNode.add(textFieldCount, 1, rowC++);

            final Label labelOperator = new Label("Operator");
            final ChoiceBox<Operators> choiseBoxOperator = new ChoiceBox<>();
            choiseBoxOperator.getItems().setAll(Operators.values());
            choiseBoxOperator.setValue(bpmnElementCountRule.getOperator());
            choiseBoxOperator.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnElementCountRule.setOperator(newValue));
            ruleNode.add(labelOperator, 0, rowC);
            ruleNode.add(choiseBoxOperator, 1, rowC++);

            final Label labelElement = new Label("Element Type");
            final ChoiceBox<Class<? extends ModelElementInstance>> choiseBoxElement = createElementChoiceBox();
            choiseBoxElement.setValue(bpmnElementCountRule.getElementClass());
            choiseBoxElement.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> bpmnElementCountRule.setElementClass(newValue));

            ruleNode.add(labelElement, 0, rowC);
            ruleNode.add(choiseBoxElement, 1, rowC++);


        }

        HBox ruleActions = new HBox();
        ruleActions.setSpacing(10);

        Button deleteRule = createButton("Delete");
        deleteRule.setOnAction(event -> {
            bpmnRuleEngine.getRules().getRules().remove(rule);
            refreshRuleList();
        });
        ruleActions.getChildren().add(deleteRule);

        Button disableRule = createButton(rule.isActive() ? "Disable" : "Enable");
        disableRule.setOnAction(event -> {
            rule.setActive(!rule.isActive());
            disableRule.setText(rule.isActive() ? "Disable" : "Enable");
            rulePane.setStyle(rule.isActive() ? "" : "-fx-color: #ffab91");
        });
        ruleActions.getChildren().add(disableRule);

        ruleNode.add(ruleActions, 1, rowC);

        return ruleNode;

    }

    /**
     * Create a new Tab and start the BPMN test for the given BPMN Modell
     * @param filename - Path of the BPMN Model which should get analysed
     */
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
                List<ValidationResult> resultsRules = bpmnRuleEngine.analyseModelValidation(modelInstanceOpt.get(), logService);

                if (resultsRules != null) {
                    ObservableList<ValidationResult> resultListFX = FXCollections.observableArrayList();
                    resultListFX.setAll(resultsRules);
                    tableViewValidation.setItems(resultListFX);
                }

                List<MetricResult> resultsMetrics = bpmnRuleEngine.analyseModelMetrics(modelInstanceOpt.get(), logService);

                if (resultsMetrics != null) {
                    ObservableList<MetricResult> resultListFX = FXCollections.observableArrayList();
                    resultListFX.setAll(resultsMetrics);
                    tableViewMetric.setItems(resultListFX);
                }
            } else {
                List<ValidationResult> resultsRules_XmlError = new ArrayList<>();
                BpmnXmlService bpmnXmlService = new BpmnXmlService();
                resultsRules_XmlError.add(new ValidationResult(new BpmnXmlValidationRule("XML Error", "The model failed to loaded, try xml validation", "", ""), false, bpmnXmlService.validateXML(filename.toAbsolutePath().toString())));
                ObservableList<ValidationResult> resultListFX = FXCollections.observableArrayList();
                resultListFX.setAll(resultsRules_XmlError);
                tableViewValidation.setItems(resultListFX);
            }

        }).start();

    }


    /**
     * Helper function - create a new horizontal SplitPane
     */
    public SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        return splitPane;
    }

    /**
     * Helper function - create a new Tab
     */
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

    /**
     * Create a Result TableView for rules
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

    /**
     * Create a Result TableView for metrics
     */
    private TableView createMertricResultTable() {
        TableView<MetricResult> table = new TableView<>();
        table.setEditable(false);

        TableColumn<MetricResult, String> nameColum = new TableColumn<>("Metric name");
        nameColum.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getName()));

        TableColumn<MetricResult, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getDescription()));

        TableColumn<MetricResult, String> resultColumn = new TableColumn<>("Result");
        resultColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue() + ""));

        TableColumn<MetricResult, String> trendColumn = new TableColumn<>("Trend");
        trendColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getTrend().toString()));

        TableColumn<MetricResult, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBpmnMetric().getRef()));

        table.getColumns().addAll(nameColum, descColumn, resultColumn, trendColumn, sourceColumn);

        return table;
    }

    /**
     * Create a choisebox for BPMN Elements
     */
    private ChoiceBox<Class<? extends ModelElementInstance>> createElementChoiceBox() {
        ChoiceBox<Class<? extends ModelElementInstance>> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().add(Event.class);
        choiceBox.getItems().add(StartEvent.class);
        choiceBox.getItems().add(EndEvent.class);
        choiceBox.getItems().add(IntermediateCatchEvent.class);
        choiceBox.getItems().add(IntermediateThrowEvent.class);

        choiceBox.getItems().add(Gateway.class);
        choiceBox.getItems().add(ExclusiveGateway.class);
        choiceBox.getItems().add(ParallelGateway.class);
        choiceBox.getItems().add(EventBasedGateway.class);
        choiceBox.getItems().add(ComplexGateway.class);
        choiceBox.getItems().add(InclusiveGateway.class);

        choiceBox.getItems().add(Activity.class);
        choiceBox.getItems().add(Task.class);
        choiceBox.getItems().add(SubProcess.class);

        choiceBox.getItems().add(Participant.class);

        choiceBox.getItems().add(SequenceFlow.class);
        choiceBox.getItems().add(MessageFlow.class);

        choiceBox.getItems().add(DataObject.class);
        choiceBox.getItems().add(DataStore.class);
        choiceBox.getItems().add(Message.class);

        choiceBox.getItems().add(FlowNode.class);
        choiceBox.getItems().add(FlowNode.class);

        choiceBox.getItems().add(FlowNode.class);

        choiceBox.getItems().add(ModelElementInstance.class);
        return choiceBox;
    }

    /**
     * Create a choisebox for BPMN FlowNodes
     */
    private ChoiceBox<Class<? extends FlowNode>> createFlowNodeChoiceBox() {
        ChoiceBox<Class<? extends FlowNode>> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().add(Event.class);
        choiceBox.getItems().add(StartEvent.class);
        choiceBox.getItems().add(EndEvent.class);
        choiceBox.getItems().add(IntermediateCatchEvent.class);
        choiceBox.getItems().add(IntermediateThrowEvent.class);

        choiceBox.getItems().add(Gateway.class);
        choiceBox.getItems().add(ExclusiveGateway.class);
        choiceBox.getItems().add(ParallelGateway.class);
        choiceBox.getItems().add(EventBasedGateway.class);
        choiceBox.getItems().add(ComplexGateway.class);
        choiceBox.getItems().add(InclusiveGateway.class);

        choiceBox.getItems().add(Activity.class);
        choiceBox.getItems().add(Task.class);
        choiceBox.getItems().add(SubProcess.class);

        choiceBox.getItems().add(FlowNode.class);

        return choiceBox;
    }

    /**
     * Helperfunction to create a simple button with a given label
     */
    private Button createButton(String label) {
        Button button = new Button(label);
        button.setPadding(new Insets(5, 5, 5, 5));
        button.autosize();
        return button;
    }

}
