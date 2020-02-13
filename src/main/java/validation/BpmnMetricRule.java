package validation;

import enums.Operators;
import meric.BpmnMetric;

import meric.ElementCountMetric;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "bpmnMetricRule")
public class BpmnMetricRule extends BpmnRule {

    private double amount;
    private Operators operator;


    private BpmnMetric metric;

    public BpmnMetricRule() {
    }

    public BpmnMetricRule(String name, String description, String ref, int amount, Operators operator, BpmnMetric metric) {
        super(name, description, ref);
        this.amount = amount;
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
                valid = metricValue < amount;
                break;
            case lessEqual:
                valid = metricValue <= amount;
                break;
            case equal:
                valid = metricValue == amount;
                break;
            case moreEqual:
                valid = metricValue >= amount;
                break;
            case more:
                valid = metricValue > amount;
                break;
        }

        ValidationResult result = new ValidationResult(this, valid);
        if (!valid) {
            result.addError("The metric " + metric.getName() + " value for the model is " + metricValue + "! Rule: " + operator + " than " + amount);
        }
        return result;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Operators getOperator() {
        return operator;
    }

    public void setOperator(Operators operator) {
        this.operator = operator;
    }

    @XmlElements({
            @XmlElement(type = ElementCountMetric.class, name = "elementCountMetric")})
    public BpmnMetric getMetric() {
        return metric;
    }

    public void setMetric(BpmnMetric metric) {
        this.metric = metric;
    }
}
