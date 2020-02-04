package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public interface BpmnRule {
    /**
     * Validate the BpmnRule with a given BpmnModel
     * @param modelInstance - Model which should be validated
     * @return boolean - True if the rule has been validated
     */
     boolean validate(BpmnModelInstance modelInstance);

    /**
     * Convert the BpmnRule to a string
     * @return String - Description of the rule
     */
    String toString();
}
