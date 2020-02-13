package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "bpmnOntologyValidationRule")
@Deprecated
public class BpmnOntologyValidationRule extends BpmnRule {

    private String ontologyPath;

    public BpmnOntologyValidationRule() {
    }

    public BpmnOntologyValidationRule(String name, String description, String ref, String ontologyPath) {
        super(name, description, ref);
        this.ontologyPath = ontologyPath;
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        List<String> errors = new ArrayList<>();
        errors.add("Not implemented");
        return new ValidationResult(this, false, errors);
    }

    public String getOntologyPath() {
        return ontologyPath;
    }

    public void setOntologyPath(String ontologyPath) {
        this.ontologyPath = ontologyPath;
    }
}
