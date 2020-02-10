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
        this.path = path;
        this.uuid = UUID.randomUUID();
    }

    public BpmnToken(BpmnToken bpmnToken) {
        this.currentFlowNode = bpmnToken.getCurrentFlowNode();
        this.path = new ArrayList<>(bpmnToken.getPath());
        this.uuid = UUID.randomUUID();
    }

    public boolean moveToken(FlowNode flowNode){
        if(flowNode == null) throw new NullPointerException();
        if (!path.stream().map(BaseElement::getId).collect(Collectors.toList()).contains(flowNode.getId()))
        {
            path.add(this.currentFlowNode);
            this.currentFlowNode = flowNode;
            return true;
        }
        else {
            return false;
        }
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