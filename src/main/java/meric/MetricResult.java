package meric;

public class MetricResult {
    private int value;
    private Trend trend;
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

    public Trend getTrend() {
        return trend;
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }

    public BpmnMetric getBpmnMetric() {
        return bpmnMetric;
    }

    public void setBpmnMetric(BpmnMetric bpmnMetric) {
        this.bpmnMetric = bpmnMetric;
    }
}
