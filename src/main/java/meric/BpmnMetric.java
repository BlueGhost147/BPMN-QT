package meric;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public abstract class BpmnMetric {
    private String name = "";
    private String description = "";
    private String ref = "";
    private Trend trend;

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
}
