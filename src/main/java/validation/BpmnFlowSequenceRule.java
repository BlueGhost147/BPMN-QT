package validation;

import enums.SequenceRuleType;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "bpmnFlowSequenceRule")
public class BpmnFlowSequenceRule extends BpmnRule {
    private Class<? extends FlowNode> elementClass;
    private Class<? extends FlowNode> previousElementClass;
    private SequenceRuleType sequenceRuleType;

    @XmlTransient
    private BpmnService bpmnService;

    public BpmnFlowSequenceRule() {
        this.bpmnService = new BpmnService();
    }

    public BpmnFlowSequenceRule(String name, String description, String ref, Class<? extends FlowNode> elementClass, Class<? extends FlowNode> previousElementClass, SequenceRuleType sequenceRuleType) {
        super(name, description, ref);
        this.elementClass = elementClass;
        this.previousElementClass = previousElementClass;
        this.sequenceRuleType = sequenceRuleType;
        this.bpmnService = new BpmnService();
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        List<String> errors = new ArrayList<>();

        bpmnService.findElementsByType(modelInstance, elementClass)
                .forEach(flowNode -> {
                    ((FlowNode) flowNode).getIncoming().forEach(
                            incomingSF -> {
                                FlowNode source = incomingSF.getSource();
                                if (sequenceRuleType == SequenceRuleType.NOT_ALLOWED_PREDECESSOR) {
                                    if (previousElementClass.isInstance(source)) {
                                        errors.add("The element " + ((FlowNode) flowNode).getName() + " has the predecessor " + source.getName() + " of type " + source.getClass().getName());
                                    }
                                } else if (sequenceRuleType == SequenceRuleType.NEEDS_PREDECESSOR) {
                                    if (! previousElementClass.isInstance(source)) {
                                        errors.add("The element " + ((FlowNode) flowNode).getName() + " has the predecessor " + source.getName() + " of type " + source.getClass().getName());

                                    }
                                }
                            }
                    );
                });

        return new ValidationResult(this, errors.size() == 0, errors);
    }

    public Class<? extends FlowNode> getElementClass() {
        return elementClass;
    }

    public void setElementClass(Class<? extends FlowNode> elementClass) {
        this.elementClass = elementClass;
    }

    public Class<? extends FlowNode> getPreviousElementClass() {
        return previousElementClass;
    }

    public void setPreviousElementClass(Class<? extends FlowNode> previousElementClass) {
        this.previousElementClass = previousElementClass;
    }

    public SequenceRuleType getSequenceRuleType() {
        return sequenceRuleType;
    }

    public void setSequenceRuleType(SequenceRuleType sequenceRuleType) {
        this.sequenceRuleType = sequenceRuleType;
    }
}
