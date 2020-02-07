package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.xml.ModelException;
import service.BpmnService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verzweigungen von Sequenzflüssen aus einer Aktivität erfolgen nicht direkt sondern über einen Gateway
 */
public class GatewayMergeRule extends BpmnRule {

    private BpmnService bpmnService;

    public GatewayMergeRule(String name, String description, String ref) {
        super(name, description, ref);
        bpmnService = new BpmnService();
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        try {
            List<String> errors = bpmnService.findElementsByType(modelInstance, FlowNode.class).stream()
                    // Check all FlowNodes, except Gateways
                    .filter(flowNode -> !(flowNode instanceof Gateway))
                    .filter(flowNode -> ((FlowNode) flowNode).getIncoming().size() > 1 ||
                            ((FlowNode) flowNode).getOutgoing().size() > 1)
                    .map(flowNode -> "The element " + ((FlowNode) flowNode).getName() + " of the type " + flowNode.getClass() + " is used to merge or split the sequence flow")
                    .collect(Collectors.toList());

            return new ValidationResult(this, errors.size() == 0, errors);
        }
        catch (ModelException e)
        {
            List<String> errors = new ArrayList<>();
            errors.add(e.getMessage());
            return new ValidationResult(this, false, errors);
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
