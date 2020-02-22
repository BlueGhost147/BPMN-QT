package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.xml.ModelException;
import service.BpmnService;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verzweigungen von Sequenzflüssen aus einer Aktivität erfolgen nicht direkt sondern über einen Gateway
 */
@XmlRootElement(name = "bpmnGatewayMergeRule")
public class BpmnGatewayMergeRule extends BpmnRule {

    @XmlTransient
    private BpmnService bpmnService;

    public BpmnGatewayMergeRule() {
        bpmnService = new BpmnService();
    }

    public BpmnGatewayMergeRule(String name, String description, String ref) {
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
                    .map(flowNode -> "The element " + ((FlowNode) flowNode).getName() + " of the type " + flowNode.getClass().getSimpleName() + " is used to merge or split the sequence flow")
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

    public BpmnService getBpmnService() {
        return bpmnService;
    }

    public void setBpmnService(BpmnService bpmnService) {
        this.bpmnService = bpmnService;
    }

}
