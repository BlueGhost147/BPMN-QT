package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "bpmnSoundnessRule")
public class BpmnSoundnessRule extends BpmnRule {

    private int soundnessCategory;

    public BpmnSoundnessRule() {
    }

    public BpmnSoundnessRule(String name, String description, String ref) {
        super(name, description, ref);
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        List<String> errors = new ArrayList<>();
        errors.add("Not implemented");
        return new ValidationResult(this, false, errors);
    }
}
