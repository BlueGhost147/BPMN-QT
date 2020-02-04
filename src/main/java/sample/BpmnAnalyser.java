package sample;

import enums.Operators;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;

import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import service.BpmnModelService;
import service.BpmnService;
import service.BpmnXmlService;
import service.LogService;
import validation.BpmnElementCountRule;
import validation.BpmnRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class BpmnAnalyser {

    private BpmnModelService bpmnModelService;
    private BpmnService bpmnService;
    private BpmnXmlService bpmnXmlService;

    public BpmnAnalyser() {
        bpmnModelService = new BpmnModelService();
        bpmnService = new BpmnService();
        bpmnXmlService = new BpmnXmlService();
    }

    public void analyseModel(String modelPath) {
        LogService.logEvent("BpmnAnalyser", "Start check for " + modelPath);
        LogService.logLine("BpmnAnalyser");

        bpmnXmlService.validateXML(modelPath);

        Optional<BpmnModelInstance> bpmnModelOptional = bpmnModelService.loadDiagram(modelPath);
        if (bpmnModelOptional.isPresent()) {
            BpmnModelInstance bpmnModelInstance = bpmnModelOptional.get();

            int activityCount = bpmnService.findElementsByType(bpmnModelInstance, Activity.class).size();
            LogService.logEvent("BpmnAnalyser", "Activity Count: " + activityCount);

            testRuleEngine(bpmnModelInstance);
        }
    }

    private void testRuleEngine(BpmnModelInstance bpmnModelInstance) {
        List<BpmnRule> rules = new ArrayList<>();
        rules.add(new BpmnElementCountRule(1, Operators.moreEqual, StartEvent.class));
        rules.add(new BpmnElementCountRule(2, Operators.moreEqual, EndEvent.class));
        rules.forEach(bpmnRule -> {
            boolean validationResult = bpmnRule.validate(bpmnModelInstance);
            LogService.logEvent("BpmnAnalyser", bpmnRule.toString() + " result: " + validationResult);
        });
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
