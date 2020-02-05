package validation;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> errorMsg;

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errorMsg = new ArrayList<>();
    }

    public ValidationResult(boolean valid, List<String> errorMsg) {
        this.valid = valid;
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

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errorMsg=" + errorMsg +
                ", errorMsg=" + errorMsg.size() +
                '}';
    }
}
