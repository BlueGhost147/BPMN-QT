package meric;

public class MetricResult {
    private int value;
    private BpmnMetric bpmnMetric;

    public MetricResult(int value, BpmnMetric bpmnMetric) {
        this.value = value;
        this.bpmnMetric = bpmnMetric;
    }

    public int getValue() {
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
