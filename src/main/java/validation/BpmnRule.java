package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public abstract class BpmnRule implements Serializable {

    private String name = "";
    private String description = "";
    private String ref = "";
    private boolean active = true;

    public BpmnRule() {
    }

    public BpmnRule(String name) {
        this.name = name;
    }

    public BpmnRule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public BpmnRule(String name, String description, String ref) {
        this.name = name;
        this.description = description;
        this.ref = ref;
    }

    /**
     * Validate the BpmnRule with a given BpmnModel
     *
     * @param modelInstance - Model which should be validated
     * @return boolean - True if the rule has been validated
     */
   public abstract ValidationResult validate(BpmnModelInstance modelInstance);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name + " - " + description;
    }
}
