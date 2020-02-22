package main;

import enums.Operators;
import enums.RuleOperator;
import enums.SequenceRuleType;
import meric.*;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnModelService;
import service.BpmnService;
import service.BpmnXmlService;
import service.LogService;
import validation.*;

import java.util.ArrayList;
import java.util.List;

public class BpmnRuleEngine {

    private BpmnModelService bpmnModelService;
    private BpmnService bpmnService;
    private BpmnXmlService bpmnXmlService;

    private RuleList rules;
    private List<BpmnMetric> metrics;

    public BpmnRuleEngine() {
        bpmnModelService = new BpmnModelService();
        bpmnService = new BpmnService();
        bpmnXmlService = new BpmnXmlService();
        rules = new RuleList();
        metrics = new ArrayList<>();
        // createTestRuleEngine();
        createTestMetrics();
    }

    public List<ValidationResult> analyseModelValidation(BpmnModelInstance bpmnModelInstance, LogService logService) {
        logService.logEvent("BpmnRuleEngine", "Start rule engine");
        List<ValidationResult> results = validateModel(bpmnModelInstance, logService);
        if (results != null)
            logService.logEvent("BpmnRuleEngine", "Finished validation, validated " + results.size() + " rules!");

        return results;
    }

    public List<MetricResult> analyseModelMetrics(BpmnModelInstance bpmnModelInstance, LogService logService) {
        logService.logEvent("BpmnRuleEngine", "Start metric engine");
        List<MetricResult> results = calculateMetrics(bpmnModelInstance, logService);
        if (results != null)
            logService.logEvent("BpmnRuleEngine", "Finished metric calculation, calculated " + results.size() + " metrics!");

        return results;
    }

    public void createTestRules() {
        rules = new RuleList();

        rules.getRules().add(new BpmnXmlValidationRule("Xml Schema valid", "The BPMN XML is valid", "https://www.omg.org/spec/BPMN/20100501/BPMN20.xsd", "https://www.omg.org/spec/BPMN/20100501/BPMN20.xsd"));

        BpmnRule countRule1 = new BpmnElementCountRule(
                "Start Event existiert",
                "Jeder Prozess braucht mindestends ein StartEvent",
                "",
                1, Operators.moreEqual, StartEvent.class);
        rules.getRules().add(countRule1);

        BpmnRule countRule2 = new BpmnElementCountRule(
                "End Event existiert",
                "Jeder Prozess braucht mindestends ein EndEvent",
                "",
                1, Operators.moreEqual, EndEvent.class);
        rules.getRules().add(countRule2);

        rules.getRules().add(new BpmnElementCountRule(
                "Modellierungskonventionen - Aktivitäten",
                "Maximal 9 – 15 Aktivitäten (Ausprägungen siehe Kapitel 4.6) pro Diagramm.",
                "https://www.ech.ch/de/dokument/fb5725cb-813f-47dc-8283-c04f9311a5b8",
                15, Operators.lessEqual, Activity.class));

        List<BpmnRule> rulesClone = new ArrayList<>();
        rulesClone.add(countRule1);
        rulesClone.add(countRule2);

        BpmnComplexRule complexRule1 = new BpmnComplexRule("Complex test rule 1", "test ", "", new ArrayList<>(rulesClone), RuleOperator.AND);
        complexRule1.setActive(false);
        rules.getRules().add(complexRule1);

        BpmnComplexRule complexRule2 = new BpmnComplexRule("Complex test rule 2", "test", "", new ArrayList<>(rulesClone), RuleOperator.OR);
        complexRule2.setActive(false);
        rules.getRules().add(complexRule2);

        rules.getRules().add(new BpmnGatewayMergeRule("Verzweigungen über Gateways", "Verzweigungen von Sequenzflüssen aus einer Aktivität erfolgen nicht direkt sondern über einen Gateway", "https://www.ech.ch/de/dokument/fb5725cb-813f-47dc-8283-c04f9311a5b8"));

        rules.getRules().add(new BpmnPoolProcessRule("Pool besitzen einen korrekten Prozess", "In jedem aufgeklappten Pool wird genau ein vollständiger Prozess modelliert", "https://www.ech.ch/de/dokument/fb5725cb-813f-47dc-8283-c04f9311a5b8"));

        rules.getRules().add(new BpmnSoundnessRule("BPMN Modell Soundness", "Korrektheit des Sequenzflusses", ""));


        if(metrics != null && metrics.size()>3) {
            BpmnMetric bpmnMetric = metrics.get(3);
            rules.getRules().add(new BpmnMetricRule("Max Flow Count", "Nicht mehr als 100 Flows", "", 100, Operators.less, bpmnMetric));
        }
        rules.getRules().add(new BpmnFlowSequenceRule("Kein direkter Sequenzfluss von StartEvent zu EndEvent","","",
                EndEvent.class,StartEvent.class,SequenceRuleType.NOT_ALLOWED_PREDECESSOR));

        rules.getRules().add(new BpmnFlowSequenceRule("Kein direkter Sequenzfluss von StartEvent zu ExclusiveGateway","","",
                ExclusiveGateway.class,StartEvent.class,SequenceRuleType.NOT_ALLOWED_PREDECESSOR));
    }

    private void createTestMetrics() {
        metrics = new ArrayList<>();

        List<Class<? extends ModelElementInstance>> noaClasses = new ArrayList<>();
        noaClasses.add(Activity.class);
        metrics.add(new ElementCountMetric("NOA", "Number of activities", "J. Cardoso, J. Mendling, and H. A. Reijers,\n" +
                "“A discourse on complexity of process models,”\n" +
                "Proceedings of the 2006 international conference\n" +
                "on Business Process Management Workshops\n" +
                "(BPM’06), 2006, pp. 117–128.", Trend.LESS_BETTER, noaClasses));

        List<Class<? extends ModelElementInstance>> noasjClasses = new ArrayList<>();
        noasjClasses.add(Activity.class);
        noasjClasses.add(ParallelGateway.class);
        metrics.add(new ElementCountMetric("NOASJ", "Number of activities, splits and joins", "J. Cardoso, J. Mendling, and H. A. Reijers,\n" +
                "“A discourse on complexity of process models,”\n" +
                "Proceedings of the 2006 international conference\n" +
                "on Business Process Management Workshops\n" +
                "(BPM’06), 2006, pp. 117–128.", Trend.LESS_BETTER, noasjClasses));

        List<Class<? extends ModelElementInstance>> noeClasses = new ArrayList<>();
        noeClasses.add(Event.class);
        metrics.add(new ElementCountMetric("NOE", "Number of events", "", Trend.LESS_BETTER, noeClasses));

        List<Class<? extends ModelElementInstance>> flowNodeClasses = new ArrayList<>();
        flowNodeClasses.add(FlowNode.class);
        metrics.add(new ElementCountMetric("NOFn", "Number of flow nodes", "", Trend.LESS_BETTER, flowNodeClasses));

        metrics.add(new SequenceFlowSplitMetric("SequenceFlow split count","Gives a indication how complex the sequence flow is by dividing the amount of sequence flows through the amount of flow nodes(start & end * 0.5)","",Trend.LESS_BETTER));
        metrics.add(new CognitiveWeightsMetric("Cognitive complexity measure","Cognitive effort required t understand the model","V. Gruhn and R. Laue, “Adopting the Cognitive\n" +
                "Complexity Measure for Business Process\n" +
                "Models,” 5th IEEE International Conference on\n" +
                "Cognitive Informatics, 2006. ICCI 2006, Vol. 1.\n" +
                "IEEE, 2006b, pp. 236–241. \n----------changed by----------\nM. Sadowska, „An Approach to Assessing the Quality of Business Process Models Expressed in BPMN,“ e-Informatica Software Engineering Journal\nBd. 9, Nr. 1, pp. 55-77, 2015.  ",Trend.LESS_BETTER));
    }

    public List<ValidationResult> validateModel(BpmnModelInstance bpmnModelInstance, LogService logService) {
        List<ValidationResult> results = new ArrayList<>();


        rules.getRules()
                .stream().filter(BpmnRule::isActive)
                .forEach(bpmnRule -> {
                    logService.logEvent("BpmnRuleEngine", "Try to validate " + bpmnRule.getName());
                    ValidationResult validationResult = bpmnRule.validate(bpmnModelInstance);
                    results.add(validationResult);

                    if (validationResult.isValid()) {
                        logService.logEvent("BpmnRuleEngine", "Validated " + bpmnRule.getName() + " successfully!");
                    } else {
                        logService.logEvent("BpmnRuleEngine", "Validation for " + bpmnRule.getName() + " failed!\nErrors:\n" + validationResult.getFullErrorMsg());
                    }

                });

        return results;
    }

    public List<MetricResult> calculateMetrics(BpmnModelInstance bpmnModelInstance, LogService logService) {
        List<MetricResult> results = new ArrayList<>();
        metrics.forEach(metric -> {
            MetricResult metricResult = metric.calculate(bpmnModelInstance);
            results.add(metricResult);

            logService.logEvent("BpmnRuleEngine", "Calculated " + metric.getName() + " - " + metricResult.getValue());
        });

        return results;
    }

    public RuleList getRules() {
        return rules;
    }

    public void setRules(RuleList rules) {
        this.rules = rules;
    }

    public List<BpmnMetric> getMetrics() {
        return metrics;
    }
}
