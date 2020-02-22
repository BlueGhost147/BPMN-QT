package validation;

import enums.RuleOperator;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * BpmnComplexRule -> BpmnRule which is build from a set of rules
 */
@XmlRootElement(name = "bpmnComplexRule")
public class BpmnComplexRule extends BpmnRule {

    private List<BpmnRule> bpmnRuleList;
    private RuleOperator operator;

    public BpmnComplexRule() {
        bpmnRuleList = new ArrayList<>();
    }

    public BpmnComplexRule(String name, String description, String ref, RuleOperator operator) {
        super(name, description, ref);
        this.bpmnRuleList = new ArrayList<>();
        this.operator = operator;
    }

    public BpmnComplexRule(String name, String description, String ref, List<BpmnRule> bpmnRuleList, RuleOperator operator) {
        super(name, description, ref);
        this.bpmnRuleList = bpmnRuleList;
        this.operator = operator;
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {

        boolean valid = false;
        List<String> errors = new ArrayList<>();

        switch (operator) {
            case AND:
                valid = true;
                for (BpmnRule rule : bpmnRuleList) {
                    if(rule == this) {
                        errors.add("Recursion in complex rule detected: "+rule.getName() + " was ignored");
                        continue;
                    }
                    ValidationResult ruleResult = rule.validate(modelInstance);
                    errors.addAll(ruleResult.getErrorMsg());
                    if (!ruleResult.isValid()) valid = false;
                }
                break;
            case OR:
                valid = false;
                for (BpmnRule rule : bpmnRuleList) {
                    if(rule == this) {
                        errors.add("Recursion in complex rule detected: "+rule.getName() + " was ignored");
                        continue;
                    }
                    ValidationResult ruleResult = rule.validate(modelInstance);
                    errors.addAll(ruleResult.getErrorMsg());
                    if (ruleResult.isValid()) valid = true;
                }
                /*
                valid = bpmnRuleList.stream().anyMatch(rule -> {
                    ValidationResult ruleResult = rule.validate(modelInstance);
                    errors.addAll(ruleResult.getErrorMsg());
                    return ruleResult.isValid();
                });*/
                break;
        }

        return new ValidationResult(this, valid, errors);
    }

    public List<BpmnRule> getBpmnRuleList() {
        return bpmnRuleList;
    }

    public void setBpmnRuleList(List<BpmnRule> bpmnRuleList) {
        this.bpmnRuleList = bpmnRuleList;
    }

    public RuleOperator getOperator() {
        return operator;
    }

    public void setOperator(RuleOperator operator) {
        this.operator = operator;
    }

    public void addBpmnRule(BpmnRule bpmnRule) {
        this.bpmnRuleList.add(bpmnRule);
    }
}
