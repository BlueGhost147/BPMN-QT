package meric;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "elementCountMetric")
public class ElementCountMetric extends BpmnMetric implements Serializable {

    @XmlTransient
    private BpmnService bpmnService;

    private List<Class<? extends ModelElementInstance>> elementTypes;

    public ElementCountMetric() {
        this.bpmnService = new BpmnService();
    }

    public ElementCountMetric(String name, String description, String ref, Trend trend, List<Class<? extends ModelElementInstance>> elementTypes) {
        super(name, description, ref, trend);
        this.elementTypes = elementTypes;
        bpmnService = new BpmnService();
    }

    @Override
    public MetricResult calculate(BpmnModelInstance bpmnModelInstance) {
        int elementCount = elementTypes.stream().mapToInt(elementType -> bpmnService.findElementsByType(bpmnModelInstance, elementType).size()).sum();
        return new MetricResult(elementCount, this);
    }

    public List<Class<? extends ModelElementInstance>> getElementTypes() {
        return elementTypes;
    }

    public void setElementTypes(List<Class<? extends ModelElementInstance>> elementTypes) {
        this.elementTypes = elementTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ElementCountMetric)) return false;
        if (!super.equals(o)) return false;
        ElementCountMetric that = (ElementCountMetric) o;
        return Objects.equals(getElementTypes(), that.getElementTypes());
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), getElementTypes());
    }
}
