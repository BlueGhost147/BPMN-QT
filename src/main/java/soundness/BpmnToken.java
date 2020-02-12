package soundness;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class BpmnToken {
    private FlowNode currentFlowNode;
    private List<FlowNode> path;
    private UUID uuid;

    public BpmnToken(FlowNode currentFlowNode) {
        this.currentFlowNode = currentFlowNode;
        this.path = new ArrayList<>();
        this.uuid = UUID.randomUUID();
    }

    public BpmnToken(FlowNode currentFlowNode, List<FlowNode> path) {
        this.currentFlowNode = currentFlowNode;
        this.path = new ArrayList<>(path);
        this.uuid = UUID.randomUUID();
    }

    public BpmnToken(BpmnToken bpmnToken) {
        this.currentFlowNode = bpmnToken.getCurrentFlowNode();
        this.path = new ArrayList<>(bpmnToken.getPath());
        this.uuid = UUID.randomUUID();
    }

    public BpmnToken moveAndCloneToken(BpmnToken bpmnToken, FlowNode flowNode) {
        BpmnToken newBpmnToken = new BpmnToken(bpmnToken);
        newBpmnToken.getPath().add(newBpmnToken.getCurrentFlowNode());
        newBpmnToken.setCurrentFlowNode(flowNode);
        return newBpmnToken;
    }

    private void setCurrentFlowNode(FlowNode currentFlowNode) {
        this.currentFlowNode = currentFlowNode;
    }

    public FlowNode getCurrentFlowNode() {
        return currentFlowNode;
    }

    public List<FlowNode> getPath() {
        return path;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmnToken)) return false;
        BpmnToken bpmnToken = (BpmnToken) o;
        return Objects.equals(getCurrentFlowNode(), bpmnToken.getCurrentFlowNode());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getCurrentFlowNode());
    }
}
