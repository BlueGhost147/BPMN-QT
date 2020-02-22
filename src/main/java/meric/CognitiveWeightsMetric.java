package meric;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sources:
 * V. Gruhn and R. Laue, “Adopting the Cognitive
 * Complexity Measure for Business Process
 * Models,” 5th IEEE International Conference on
 * Cognitive Informatics, 2006. ICCI 2006, Vol. 1.
 * IEEE, 2006b, pp. 236–241.
 * <p>
 * M. Sadowska, „An Approach to Assessing the Quality of Business Process Models Expressed in BPMN,“
 * e-Informatica Software Engineering Journal,
 * Bd. 9, Nr. 1,
 * pp. 55-77, 2015.
 */
@XmlRootElement(name = "cognitiveWeightsMetric")
public class CognitiveWeightsMetric extends BpmnMetric implements Serializable {

    @XmlTransient
    private BpmnService bpmnService;


    public CognitiveWeightsMetric() {
        this.bpmnService = new BpmnService();
    }

    public CognitiveWeightsMetric(String name, String description, String ref, Trend trend) {
        super(name, description, ref, trend);
        bpmnService = new BpmnService();
    }

    @Override
    public MetricResult calculate(BpmnModelInstance bpmnModelInstance) {
        Collection<ModelElementInstance> flowNodes = bpmnService.findElementsByType(bpmnModelInstance, FlowNode.class);

        final int andWeight = 4;

        int complexity = flowNodes.stream().mapToInt(flowNodeMEI -> {
            FlowNode flowNode = (FlowNode) flowNodeMEI;

            if (flowNode instanceof ExclusiveGateway || flowNode instanceof EventBasedGateway) {
                if (flowNode.getOutgoing().size() < 2)
                    // Assumption -> If a Gateway has no outgoing (soundness problem) -> just use 1
                    return 1;
                else if (flowNode.getOutgoing().size() == 2)
                    return 2;
                return 3;
            } else if (flowNode instanceof SubProcess) {
                if (flowNode.getOutgoing().size() > 2)
                    // Assumption -> if SubProcess is used for an and split -> sum weight
                    return 2 + andWeight;
                return 2;
            } else if (flowNode instanceof ComplexGateway || flowNode instanceof InclusiveGateway) {
                if (flowNode.getOutgoing().size() > 2)
                    return 7;
                return 1;
            }
            if (flowNode instanceof StartEvent || flowNode instanceof EndEvent) {
                if (flowNode.getOutgoing().size() > 1)
                    // Assumption -> if Start or EndEvent is used for an and split -> sum weight
                    return andWeight + 2;
                return 2;
            } else if (flowNode instanceof BoundaryEvent || flowNode instanceof IntermediateThrowEvent || flowNode instanceof IntermediateCatchEvent) {
                if (flowNode.getOutgoing().size() > 1)
                    // Assumption -> if IntEvent is used for an and split -> sum weight
                    return andWeight + 3;
                return 3;
            } else if (flowNode.getOutgoing().size() > 1) return andWeight;

            return 1;
        }).sum();

        return new MetricResult(complexity, this);
    }
}
