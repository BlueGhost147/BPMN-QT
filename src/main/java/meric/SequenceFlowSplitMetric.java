package meric;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "sequenceFlowSplitMetric")
public class SequenceFlowSplitMetric extends BpmnMetric implements Serializable {

    @XmlTransient
    private BpmnService bpmnService;


    public SequenceFlowSplitMetric() {
        this.bpmnService = new BpmnService();
    }

    public SequenceFlowSplitMetric(String name, String description, String ref, Trend trend) {
        super(name, description, ref, trend);
        bpmnService = new BpmnService();
    }

    @Override
    public MetricResult calculate(BpmnModelInstance bpmnModelInstance) {

        int fnCount = bpmnService.findElementsByType(bpmnModelInstance, FlowNode.class).size();
        int endCount = bpmnService.findElementsByType(bpmnModelInstance, EndEvent.class).size();
        int startCount = bpmnService.findElementsByType(bpmnModelInstance, StartEvent.class).size();
        double expectedCount = (fnCount - endCount - startCount) + (startCount + endCount) * 0.5;

        double sfCount = bpmnService.findElementsByType(bpmnModelInstance, SequenceFlow.class).size();

        double result = 0.0;

        if(expectedCount > 0)
            result =  sfCount / expectedCount;
        return new MetricResult(result, this);
    }
}
