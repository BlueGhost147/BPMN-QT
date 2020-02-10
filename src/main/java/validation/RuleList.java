package validation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "root")
public class RuleList {
    private List<BpmnRule> rules;

    public RuleList() {
        rules = new ArrayList<>();
    }

    public RuleList(List<BpmnRule> rules) {
        this.rules = rules;
    }

    public List<BpmnRule> getRules() {
        return rules;
    }

    @XmlElementWrapper(name = "rules")
    @XmlElements({
            @XmlElement(name = "bpmnComplexRule", type = BpmnComplexRule.class),
            @XmlElement(name = "bpmnElementCountRule", type = BpmnElementCountRule.class),
            @XmlElement(name = "bpmnGatewayMergeRule", type = BpmnGatewayMergeRule.class),
            @XmlElement(name = "bpmnPoolProcessRule", type = BpmnPoolProcessRule.class),
            @XmlElement(name = "bpmnXmlValidationRule", type = BpmnXmlValidationRule.class),
            @XmlElement(name = "bpmnMetricRule", type = BpmnMetricRule.class),
            @XmlElement(name = "bpmnOntologyValidationRule", type = BpmnOntologyValidationRule.class),
            @XmlElement(name = "bpmnSoundnessRule", type = BpmnSoundnessRule.class),
            @XmlElement(name = "bpmnFlowSequenceRule", type = BpmnFlowSequenceRule.class)
    })
    public void setRules(List<BpmnRule> rules) {
        this.rules = rules;
    }
}
