package sample;

import enums.Operators;
import enums.RuleOperator;
import javafx.scene.control.Tab;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;

import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
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

    public BpmnAnalyser() {
        bpmnModelService = new BpmnModelService();
        bpmnService = new BpmnService();
        bpmnXmlService = new BpmnXmlService();
        rules = new ArrayList<>();
        createTestRuleEngine();
    }

    public void analyseModel(Tab tab, String modelPath) {
        LogService.logEvent("BpmnAnalyser", "Start check for " + modelPath);
        LogService.logLine("BpmnAnalyser");

        bpmnXmlService.validateXML(modelPath);

        Optional<BpmnModelInstance> bpmnModelOptional = bpmnModelService.loadDiagram(modelPath);
        if (bpmnModelOptional.isPresent()) {
            BpmnModelInstance bpmnModelInstance = bpmnModelOptional.get();

            int activityCount = bpmnService.findElementsByType(bpmnModelInstance, Activity.class).size();
            LogService.logEvent("BpmnAnalyser", "Activity Count: " + activityCount);
        }
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
        rules.add(new BpmnComplexRule(rulesClone, RuleOperator.AND));
        rules.add(new BpmnComplexRule(rulesClone, RuleOperator.OR));

        rules.add(new GatewayMergeRule());

        rules.add(new PoolProcessRule());
    }

    public void validateModel(BpmnModelInstance bpmnModelInstance) {
        rules.forEach(bpmnRule -> {
            ValidationResult validationResult = bpmnRule.validate(bpmnModelInstance);
            LogService.logEvent("BpmnAnalyser", bpmnRule.toString() + " result: " + validationResult);
        });
    }

    public List<BpmnRule> getRules() {
        return rules;
    }

    /*
    private void customValidator(BpmnModelInstance bpmnModelInstance)
    {
        Collection<ModelElementValidator<?>> validators = new ArrayList<>();

        validators.add(new ModelElementValidator<ModelElementInstance>() {
            @Override
            public Class<ModelElementInstance> getElementType() {
                return StartEvent.class;
            }

            @Override
            public void validate(ModelElementInstance element, ValidationResultCollector validationResultCollector) {

            }
        });

        bpmnModelInstance.validate(validators);
    }*/
}
