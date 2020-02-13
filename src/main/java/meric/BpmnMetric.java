package meric;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@XmlRootElement
public abstract class BpmnMetric implements Serializable {
    private String name = "";
    private String description = "";
    private String ref = "";
    private Trend trend;

    public BpmnMetric() {
    }

    public BpmnMetric(String name, String description, String ref, Trend trend) {
        this.name = name;
        this.description = description;
        this.ref = ref;
        this.trend = trend;
    }

    public abstract MetricResult calculate(BpmnModelInstance bpmnModelInstance);

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

    public Trend getTrend() {
        return trend;
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }



    @Override
    public String toString() {
        return name + " - " + description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmnMetric)) return false;
        BpmnMetric that = (BpmnMetric) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
                Objects.equals(getRef(), that.getRef()) &&
                getTrend() == that.getTrend();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getName(), getDescription(), getRef(), getTrend());
    }
}
