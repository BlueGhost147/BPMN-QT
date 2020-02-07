package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.xml.sax.SAXException;
import service.BpmnXmlService;

import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class XmlValidationRule extends BpmnRule {

    private BpmnXmlService bpmnXmlService;
    private String xsdPath;

    public XmlValidationRule(String name, String description, String ref, String xsdPath) {
        super(name, description, ref);
        this.xsdPath = xsdPath;
        bpmnXmlService = new BpmnXmlService();
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        List<String> errors;
        DOMSource dom = modelInstance.getDocument().getDomSource();
        try {
            errors = bpmnXmlService.validateXml(dom, xsdPath).stream().map(SAXException::getMessage).collect(Collectors.toList());
            return new ValidationResult(this, errors.size() == 0, errors);
        } catch (IOException | SAXException e) {
            errors = new ArrayList<>();
            errors.add(e.getMessage());
            return new ValidationResult(this, false, errors);
        }
    }

    @Override
    public String toString() {
        return "XmlValidation - " + super.toString() + " - XSD Source: " + xsdPath;
    }

    public String getXsdPath() {
        return xsdPath;
    }

    public void setXsdPath(String xsdPath) {
        this.xsdPath = xsdPath;
    }
}
