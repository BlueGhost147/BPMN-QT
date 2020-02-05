package validation;

import enums.Operators;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

public class BpmnElementCountRule extends BpmnRule {

    private int count;
    private Operators operator;
    private Class<? extends ModelElementInstance> elementClass;

    public BpmnElementCountRule(
            String name, String desc, String ref,
            int count, Operators operator, Class<? extends ModelElementInstance> elementClass) {
        super(name,desc,ref);
        this.count = count;
        this.operator = operator;
        this.elementClass = elementClass;
    }

    /**
     * Validate how often a given element exits in the BPMN Modell
     *
     * @param modelInstance - Model which should be validated
     * @return
     */
    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        BpmnService bpmnService = new BpmnService();

        int elementCount = bpmnService.findElementsByType(modelInstance, elementClass).size();
        boolean valid = false;

        switch (operator) {
            case less:
                valid = elementCount < count;
                break;
            case lessEqual:
                valid = elementCount <= count;
                break;
            case equal:
                valid = elementCount == count;
                break;
            case moreEqual:
                valid = elementCount >= count;
                break;
            case more:
                valid = elementCount > count;
                break;
        }

        ValidationResult result = new ValidationResult(valid);
        if (!valid) {
            result.addError("The element " + elementClass + " exists " + elementCount + " times in the model! Rule: " + operator + " than " + count);
        }
        return result;

    }

    @Override
    public String toString() {
        return "BpmnElementCountRule{" +
                "count=" + count +
                ", operator=" + operator +
                ", elementClass=" + elementClass +
                '}';
    }
}
