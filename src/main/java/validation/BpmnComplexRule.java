package validation;

import enums.RuleOperator;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * BpmnComplexRule -> BpmnRule which is build from a set of rules
 */
public class BpmnComplexRule extends BpmnRule {

    private List<BpmnRule> bpmnRuleList;
    private RuleOperator operator;

    public BpmnComplexRule(List<BpmnRule> bpmnRuleList, RuleOperator operator) {
        this.bpmnRuleList = bpmnRuleList;
        this.operator = operator;
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {

        boolean valid = false;
        List<String> errors = new ArrayList<>();

        switch (operator){
            case AND:
                valid =  bpmnRuleList.stream().allMatch(rule -> {
                    ValidationResult ruleResult = rule.validate(modelInstance);
                    errors.addAll(ruleResult.getErrorMsg());
                    return ruleResult.isValid();
                });
                break;
            case OR:
                valid =  bpmnRuleList.stream().anyMatch(rule -> {
                    ValidationResult ruleResult = rule.validate(modelInstance);
                    errors.addAll(ruleResult.getErrorMsg());
                    return ruleResult.isValid();
                });
                break;
        }

        return new ValidationResult(valid, errors);
    }

    @Override
    public String toString() {
        return "BpmnComplexRule{" +
                "bpmnRuleList=" + bpmnRuleList +
                ", operator=" + operator +
                '}';
    }
}
