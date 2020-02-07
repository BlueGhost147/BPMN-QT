package sample;

import enums.Operators;
import enums.RuleOperator;
import javafx.scene.control.Tab;
import meric.BpmnMetric;
import meric.ElementCountMetric;
import meric.MetricResult;
import meric.Trend;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;

import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnModelService;
import service.BpmnService;
import service.BpmnXmlService;
import service.LogService;
import validation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class BpmnAnalyser {

    private BpmnModelService bpmnModelService;
    private BpmnService bpmnService;
    private BpmnXmlService bpmnXmlService;

    private List<BpmnRule> rules;
    private List<BpmnMetric> metrics;

    public BpmnAnalyser() {
        bpmnModelService = new BpmnModelService();
        bpmnService = new BpmnService();
        bpmnXmlService = new BpmnXmlService();
        rules = new ArrayList<>();
        metrics = new ArrayList<>();
        createTestRuleEngine();
        createTestMetrics();
    }

    public List<ValidationResult> analyseModelValidation(String modelPath, LogService logService) {
        logService.logEvent("BpmnAnalyser", "Start rule engine for: " + modelPath);

        Optional<BpmnModelInstance> bpmnModelOptional = bpmnModelService.loadDiagram(modelPath, logService);
        if (bpmnModelOptional.isPresent()) {
            BpmnModelInstance bpmnModelInstance = bpmnModelOptional.get();

            List<ValidationResult> results = validateModel(bpmnModelInstance, logService);
            if (results != null)
                logService.logEvent("BpmnAnalyser", "Finished validation for " + modelPath + " validated " + results.size() + " rules!");

            return results;
        }
        return null;
    }

    public List<MetricResult> analyseModelMetrics(String modelPath, LogService logService) {
        logService.logEvent("BpmnAnalyser", "Start rule engine for: " + modelPath);

        Optional<BpmnModelInstance> bpmnModelOptional = bpmnModelService.loadDiagram(modelPath, logService);
        if (bpmnModelOptional.isPresent()) {
            BpmnModelInstance bpmnModelInstance = bpmnModelOptional.get();

            List<MetricResult> results = calculateMetrics(bpmnModelInstance, logService);
            if (results != null)
                logService.logEvent("BpmnAnalyser", "Finished validation for " + modelPath + " validated " + results.size() + " rules!");

            return results;
        }
        return null;
    }

    private void createTestRuleEngine() {
        rules = new ArrayList<>();
        rules.add(new BpmnElementCountRule(
                "Start Event existiert",
                "Jeder Prozess braucht mindestends ein StartEvent",
                "",
                1, Operators.moreEqual, StartEvent.class));

        rules.add(new BpmnElementCountRule(
                "End Event existiert",
                "Jeder Prozess braucht mindestends ein EndEvent",
                "",
                1, Operators.moreEqual, EndEvent.class));

        rules.add(new BpmnElementCountRule(
                "Modellierungskonventionen - Aktivitäten",
                "Maximal 9 – 15 Aktivitäten (Ausprägungen siehe Kapitel 4.6) pro Diagramm.",
                "https://www.ech.ch/de/dokument/fb5725cb-813f-47dc-8283-c04f9311a5b8",
                15, Operators.less, Activity.class));

        List<BpmnRule> rulesClone = new ArrayList<>(rules);
        rules.add(new BpmnComplexRule("Complex test rule 1", "test ", "", rulesClone, RuleOperator.AND));
        rules.add(new BpmnComplexRule("Complex test rule 2", "test", "", rulesClone, RuleOperator.OR));

        rules.add(new GatewayMergeRule("Verzweigungen über Gateways", "Verzweigungen von Sequenzflüssen aus einer Aktivität erfolgen nicht direkt sondern über einen Gateway", "https://www.ech.ch/de/dokument/fb5725cb-813f-47dc-8283-c04f9311a5b8"));

        rules.add(new PoolProcessRule("Valid pool process", "In jedem aufgeklappten Pool wird genau ein vollständiger Prozess modelliert", "https://www.ech.ch/de/dokument/fb5725cb-813f-47dc-8283-c04f9311a5b8"));

        rules.add(new XmlValidationRule("Xml Schema valid", "The BPMN XML is valid", "https://www.omg.org/spec/BPMN/20100501/Semantic.xsd", "https://www.omg.org/spec/BPMN/20100501/Semantic.xsd"));
    }

    private void createTestMetrics() {
        metrics = new ArrayList<>();

        List<Class<? extends ModelElementInstance>> noaClasses = new ArrayList<>();
        noaClasses.add(Activity.class);

        metrics.add(new ElementCountMetric("NOA", "Number of activities", "k. A.", Trend.LESS_BETTER, noaClasses)
        );
    }

    public List<ValidationResult> validateModel(BpmnModelInstance bpmnModelInstance, LogService logService) {
        List<ValidationResult> results = new ArrayList<>();
        rules.forEach(bpmnRule -> {
            logService.logEvent("BpmnAnalyser", "Try to validate " + bpmnRule.getName());
            ValidationResult validationResult = bpmnRule.validate(bpmnModelInstance);
            results.add(validationResult);

            if (validationResult.isValid()) {
                logService.logEvent("BpmnAnalyser", "Validated " + bpmnRule.getName() + " successfully!");
            } else {
                logService.logEvent("BpmnAnalyser", "Validation for " + bpmnRule.getName() + " failed!\nErrors:\n" + validationResult.getFullErrorMsg());
            }

        });

        return results;
    }

    public List<MetricResult> calculateMetrics(BpmnModelInstance bpmnModelInstance, LogService logService) {
        List<MetricResult> results = new ArrayList<>();
        metrics.forEach(metric -> {
            MetricResult metricResult = metric.calculate(bpmnModelInstance);
            results.add(metricResult);

            logService.logEvent("BpmnAnalyser", "Calculated " + metric.getName() + " - "+ metricResult.getValue());
        });

        return results;
    }

    public List<BpmnRule> getRules() {
        return rules;
    }

}
