package meric;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import java.util.List;

public class ElementCountMetric extends BpmnMetric {

    private BpmnService bpmnService;

    private List<Class<? extends ModelElementInstance>> elementTypes;


    public ElementCountMetric(String name, String description, String ref, Trend trend, List<Class<? extends ModelElementInstance>> elementTypes) {
        super(name, description, ref, trend);
        this.elementTypes = elementTypes;
        bpmnService = new BpmnService();
    }

    @Override
    public MetricResult calculate(BpmnModelInstance bpmnModelInstance) {
        int elementCount = elementTypes.stream().mapToInt(elementType -> bpmnService.findElementsByType(bpmnModelInstance, elementType).size()).sum();
        return new MetricResult(elementCount, this);
    }
}
