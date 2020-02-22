package validation;

import enums.Operators;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "bpmnElementCountRule")
public class BpmnElementCountRule extends BpmnRule {

    private int count;
    private Operators operator;
    private Class<? extends ModelElementInstance> elementClass;

    public BpmnElementCountRule() {
    }

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

        ValidationResult result = new ValidationResult(this, valid);
        if (!valid) {
            result.addError("The element " + elementClass.getSimpleName() + " exists " + elementCount + " times in the model! Rule: " + operator + " than " + count);
        }
        return result;

    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Operators getOperator() {
        return operator;
    }

    public void setOperator(Operators operator) {
        this.operator = operator;
    }

    public Class<? extends ModelElementInstance> getElementClass() {
        return elementClass;
    }

    public void setElementClass(Class<? extends ModelElementInstance> elementClass) {
        this.elementClass = elementClass;
    }
}
