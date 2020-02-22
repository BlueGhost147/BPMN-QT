package meric;

public class MetricResult {
    private double value;
    private BpmnMetric bpmnMetric;

    public MetricResult(double value, BpmnMetric bpmnMetric) {
        this.value = value;
        this.bpmnMetric = bpmnMetric;
    }

    public double getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }


    public BpmnMetric getBpmnMetric() {
        return bpmnMetric;
    }

    public void setBpmnMetric(BpmnMetric bpmnMetric) {
        this.bpmnMetric = bpmnMetric;
    }
}
