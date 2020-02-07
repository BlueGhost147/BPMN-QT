package validation;

import org.camunda.bpm.model.xml.ModelInstance;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> errorMsg;
    private BpmnRule rule;
    private ModelInstance modelInstance;

    public ValidationResult(BpmnRule rule, boolean valid) {
        this.valid = valid;
        this.rule = rule;
        this.errorMsg = new ArrayList<>();
    }

    public ValidationResult(BpmnRule rule, boolean valid, List<String> errorMsg) {
        this.valid = valid;
        this.rule = rule;
        this.errorMsg = errorMsg;
    }

    public void addError(String error)
    {
        errorMsg.add(error);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrorMsg() {
        return errorMsg;
    }

    public BpmnRule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errorMsg=" + errorMsg +
                ", errorMsg=" + errorMsg.size() +
                '}';
    }

    public String getFullErrorMsg()
    {
        StringBuilder fullErrorMsg = new StringBuilder();
        for (String error : errorMsg)
        {
            fullErrorMsg.append(error).append("\n");
        }
        return fullErrorMsg.toString();
    }
}
