package soundness;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.util.*;
import java.util.stream.Collectors;

public class BpmnToken {
    private FlowNode currentFlowNode;
    private List<FlowNode> path;
    private Set<FlowNode> visitedNodes;
    private UUID uuid;

    public BpmnToken(FlowNode currentFlowNode) {
        this.currentFlowNode = currentFlowNode;
        this.path = new ArrayList<>();
        this.visitedNodes = new HashSet<>();
        this.uuid = UUID.randomUUID();
    }


    public BpmnToken(BpmnToken bpmnToken) {
        this.currentFlowNode = bpmnToken.getCurrentFlowNode();
        this.path = new ArrayList<>(bpmnToken.getPath());
        this.uuid = UUID.randomUUID();
        this.visitedNodes = new HashSet<>(bpmnToken.getVisitedNodes());
    }

    public BpmnToken moveAndCloneToken(BpmnToken bpmnToken, FlowNode flowNode) {
        BpmnToken newBpmnToken = new BpmnToken(bpmnToken);
        newBpmnToken.getPath().add(newBpmnToken.getCurrentFlowNode());
        newBpmnToken.getVisitedNodes().add(newBpmnToken.getCurrentFlowNode());
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

    public Set<FlowNode> getVisitedNodes() {
        return visitedNodes;
    }

    public void setVisitedNodes(Set<FlowNode> visitedNodes) {
        this.visitedNodes = visitedNodes;
    }

    public void joinVisitedNodes(Set<FlowNode> visitedNodesOther) {
        this.visitedNodes.addAll(visitedNodesOther);
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
