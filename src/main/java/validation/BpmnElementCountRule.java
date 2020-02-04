package validation;

import enums.Operators;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

public class BpmnElementCountRule implements BpmnRule {

    private int count;
    private Operators operator;
    private Class<? extends ModelElementInstance> elementClass;

    public BpmnElementCountRule(int count, Operators operator, Class<? extends ModelElementInstance> elementClass) {
        this.count = count;
        this.operator = operator;
        this.elementClass = elementClass;
    }

    /**
     * Validate how often a given element exits in the BPMN Modell
     * @param modelInstance - Model which should be validated
     * @return
     */
    @Override
    public boolean validate(BpmnModelInstance modelInstance) {
        BpmnService bpmnService = new BpmnService();

        int elementCount = bpmnService.findElementsByType(modelInstance, elementClass).size();

        switch (operator)
        {
            case less: return elementCount < count;
            case lessEqual: return elementCount <= count;
            case equal: return elementCount == count;
            case moreEqual: return elementCount >= count;
            case more: return elementCount > count;
            default: return false;
        }
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
