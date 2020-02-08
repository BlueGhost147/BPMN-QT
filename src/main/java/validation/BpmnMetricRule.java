package validation;

import enums.Operators;
import meric.BpmnMetric;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "bpmnMetricRule")
public class BpmnMetricRule extends BpmnRule {

    private int count;
    private Operators operator;
    private BpmnMetric metric;

    public BpmnMetricRule() {
    }

    public BpmnMetricRule(String name, String description, String ref, int count, Operators operator, BpmnMetric metric) {
        super(name, description, ref);
        this.count = count;
        this.operator = operator;
        this.metric = metric;
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        boolean valid = false;

        if(metric == null) {
            ValidationResult validationResult = new ValidationResult(this, false);
            validationResult.addError("No metric provided");
            return validationResult;
        }

        int metricValue = metric.calculate(modelInstance).getValue();

        switch (operator) {
            case less:
                valid = metricValue < count;
                break;
            case lessEqual:
                valid = metricValue <= count;
                break;
            case equal:
                valid = metricValue == count;
                break;
            case moreEqual:
                valid = metricValue >= count;
                break;
            case more:
                valid = metricValue > count;
                break;
        }

        ValidationResult result = new ValidationResult(this, valid);
        if (!valid) {
            result.addError("The metric " + metric.getName() + " value for the model is " + metricValue + "! Rule: " + operator + " than " + count);
        }
        return result;
    }
}
