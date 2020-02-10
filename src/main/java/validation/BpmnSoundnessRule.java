package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;
import soundness.BpmnToken;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.*;
import java.util.stream.Collectors;

@XmlRootElement(name = "bpmnSoundnessRule")
public class BpmnSoundnessRule extends BpmnRule {

    @XmlTransient
    private BpmnService bpmnService;

    public BpmnSoundnessRule() {
        bpmnService = new BpmnService();
    }

    public BpmnSoundnessRule(String name, String description, String ref) {
        super(name, description, ref);
        bpmnService = new BpmnService();
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {

        List<String> soundnessClass1Violations = validateSoundnessCategory1(modelInstance);
        List<String> errors = new ArrayList<>(soundnessClass1Violations);

        Collection<ModelElementInstance> pools = bpmnService.findElementsByType(modelInstance, Participant.class);

        if (pools.size() > 0) {
            pools.forEach(pool -> {
                        // List of all the flow elements of the pool
                        List<String> soundnessClass2Violations = validateSoundnessCategory2((Participant) pool);
                        errors.addAll(soundnessClass2Violations);
                    }
            );

        } else {
            List<String> soundnessClass2Violations = validateSoundnessCategory2(modelInstance);
            errors.addAll(soundnessClass2Violations);
        }


        boolean valid = errors.size() == 0;

        // errors.add("Warning: Class 2-5 not implemented yet");

        return new ValidationResult(this, valid, errors);
    }

    private List<String> validateSoundnessCategory1(BpmnModelInstance modelInstance) {
        // Find all FlowNodes of the process
        return bpmnService.findElementsByType(modelInstance, FlowNode.class).stream()
                // Filter StartEvents since they can´t have incoming SequenceFlows
                .filter(flowNode -> !(flowNode instanceof StartEvent))
                // Find all FlowNodes with no incoming SequenceFlows
                .filter(flowNodeNoStartEvents -> ((FlowNode) flowNodeNoStartEvents).getIncoming().size() == 0)
                // Create a ErrorMsg out of all found FlowNodes
                .map(flowNodeNoIncoming -> ("Soundness Class 1 violation: " + ((FlowNode) flowNodeNoIncoming).getName()) + " has no Incoming SequenceFlows")
                .collect(Collectors.toList());
    }

    private List<String> validateSoundnessCategory2(Participant pool) {
        if (pool == null || pool.getProcess() == null)
            return new ArrayList<>();

        List<String> errors = new ArrayList<>();

        List<ModelElementInstance> startEvents = pool.getProcess().getFlowElements().stream().filter(flowElement -> flowElement instanceof StartEvent).collect(Collectors.toList());
        List<ModelElementInstance> endEvents = pool.getProcess().getFlowElements().stream().filter(flowElement -> flowElement instanceof EndEvent).collect(Collectors.toList());

        if (startEvents.size() == 0) {
            errors.add("The process has no start event!");
        } else if (startEvents.size() > 1) {
            errors.add("Only one start event allowed for soundness evaluation!");
        }

        if (endEvents.size() == 0) {
            errors.add("The process has no end event!");
        }

        if (errors.size() > 0) return errors;

        StartEvent startEvent = (StartEvent) startEvents.get(0);
        List<BpmnToken> newStartList = new ArrayList<>();
        newStartList.add(new BpmnToken(startEvent));

        boolean correctEnd = checkNodePathToken(newStartList, errors, 0, new ArrayList<>());
        if (!correctEnd)
            errors.add("Process structure error detected!");

        return errors;
    }

    private List<String> validateSoundnessCategory2(BpmnModelInstance modelInstance) {
        List<String> errors = new ArrayList<>();

        List<ModelElementInstance> startEvents = new ArrayList<>(bpmnService.findElementsByType(modelInstance, StartEvent.class));
        List<ModelElementInstance> endEvents = new ArrayList<>(bpmnService.findElementsByType(modelInstance, EndEvent.class));

        if (startEvents.size() == 0) {
            errors.add("The process has no start event!");
        } else if (startEvents.size() > 1) {
            errors.add("Only one start event allowed for soundness evaluation!");
        }

        if (endEvents.size() == 0) {
            errors.add("The process has no end event!");
        }

        if (errors.size() > 0) return errors;

        StartEvent startEvent = (StartEvent) startEvents.get(0);
        List<BpmnToken> newStartList = new ArrayList<>();
        newStartList.add(new BpmnToken(startEvent));
        boolean correctEnd = checkNodePathToken(newStartList, errors, 0, new ArrayList<>());
        //boolean correctEnd = checkNodePathToken(flowNodeListToTokenList(getOutgoingFlowNodes(startEvent)), errors, 0);
        if (!correctEnd)
            errors.add("Process structure error detected!");

        return errors;
    }

    private List<FlowNode> getOutgoingFlowNodes(FlowNode flowNode) {
        return flowNode.getOutgoing().stream().map(SequenceFlow::getTarget).collect(Collectors.toList());
    }

    private List<BpmnToken> flowNodeListToTokenList(List<FlowNode> flowNodeList, List<FlowNode> path) {
        return flowNodeList.stream().map(flowNode -> new BpmnToken(flowNode, new ArrayList<>(path))).collect(Collectors.toList());
    }

    /*
    private boolean checkNodePath(List<FlowNode> activeFlowNodes, List<String> errors, int overflowCounter,
                                  Set<FlowNode> visitedNodesOld) {
        overflowCounter++;
        if (overflowCounter > 1000) {
            errors.add("Overflow counter reached, abort validation");
            return false;
        }

        // Prevent endless loops
        Set<FlowNode> visitedNodes = new HashSet<>(visitedNodesOld);
        List<FlowNode> activeFlowNodesNotLoop = new ArrayList<>(activeFlowNodes);
        activeFlowNodesNotLoop.removeAll(visitedNodes);
        List<FlowNode> activeFlowNodesLoop= new ArrayList<>(activeFlowNodes);
        activeFlowNodesLoop.removeAll(activeFlowNodesNotLoop);

        if(activeFlowNodesLoop.size() > 0)
        {
            errors.addAll(activeFlowNodesLoop.stream().map(fnLoop -> "Loop at "+fnLoop.getName()).collect(Collectors.toList()));
            activeFlowNodes.removeAll(visitedNodes);
        }

        visitedNodes.addAll(activeFlowNodes);

        boolean endCorrect = true;
        List<FlowNode> activeFlowNodesClone = new ArrayList<>(activeFlowNodes);
        for (FlowNode activeFlowNode : activeFlowNodesClone) {
            List<FlowNode> flowNodesOutgoing = getOutgoingFlowNodes(activeFlowNode);

            if (flowNodesOutgoing.size() == 0 && !(activeFlowNode instanceof EndEvent)) {
                errors.add("Process ends on non-EndEvent: " + activeFlowNode.getName());
                // The process is invalid!

                activeFlowNodes.remove(activeFlowNode);

            } else if (activeFlowNodes.size() > 1 && (activeFlowNode instanceof EndEvent)) {
                errors.add("Multiple Termination detected : " + activeFlowNode.getName());

                // Continue
                activeFlowNodes.remove(activeFlowNode);
                boolean subEndCorrect = checkNodePath(activeFlowNodes, errors, overflowCounter, visitedNodes);
                if (!subEndCorrect) endCorrect = false;
            } else if (activeFlowNode instanceof ExclusiveGateway || activeFlowNode instanceof EventBasedGateway) {

                // Exclusive decision
                for (FlowNode flowNodeOutgoing : flowNodesOutgoing) {
                    activeFlowNodes.remove(activeFlowNode);
                    List<FlowNode> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                    newActiveFlowNodes.add(flowNodeOutgoing);
                    boolean subEndCorrect = checkNodePath(newActiveFlowNodes, errors, overflowCounter, visitedNodes);
                    if (!subEndCorrect) endCorrect = false;
                }
            } else if (activeFlowNode instanceof ParallelGateway) {
                // Possible Join
                List<FlowNode> incomingFlowNodes = activeFlowNode.getIncoming().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                List<FlowNode> joinCandidates = activeFlowNodes.stream().filter(flowNode -> flowNode.getId().equals(activeFlowNode.getId())).collect(Collectors.toList());
                if (incomingFlowNodes.size() == joinCandidates.size()) {
                    List<FlowNode> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                    newActiveFlowNodes.removeAll(joinCandidates);


                    newActiveFlowNodes.remove(activeFlowNode);
                    newActiveFlowNodes.addAll(flowNodesOutgoing);

                    boolean subEndCorrect = checkNodePath(newActiveFlowNodes, errors, overflowCounter, visitedNodes);
                    if (!subEndCorrect) endCorrect = false;
                }
            } else if (!(activeFlowNode instanceof EndEvent)) {
                activeFlowNodes.remove(activeFlowNode);
                List<FlowNode> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                newActiveFlowNodes.addAll(flowNodesOutgoing);

                boolean subEndCorrect = checkNodePath(newActiveFlowNodes, errors, overflowCounter, visitedNodes);
                if (!subEndCorrect) endCorrect = false;
            } else {
                activeFlowNodes.remove(activeFlowNode);
            }
        }

        if (activeFlowNodes.size() > 0) {
            errors.add("Deadlock at " + activeFlowNodes.get(0) + (activeFlowNodes.size() > 1 ? " and " + activeFlowNodes.size() + " more" : "") + " detected!");
            return false;
        }
        return endCorrect;
    }*/

    private boolean checkNodePathToken(List<BpmnToken> activeFlowNodes, List<String> errors, int overflowCounter, List<BpmnToken> waitingFlowNodes) {
        overflowCounter++;
        if (overflowCounter > 1000) {
            errors.add("Overflow counter reached, abort validation");
            return false;
        }

        // Prevent endless loops
        for (BpmnToken activeFlowNode : activeFlowNodes) {
            int position = activeFlowNode.getPath().indexOf(activeFlowNode.getCurrentFlowNode());
            if (position >= 0) {
                // Loop detected !
                for (int i = position + 1; i < activeFlowNode.getPath().size(); i++) {
                    FlowNode flowNodeLoopPath = activeFlowNode.getPath().get(i);
                    if(flowNodeLoopPath instanceof ExclusiveGateway || flowNodeLoopPath instanceof EventBasedGateway)
                    {
                        System.out.println("Loop with possible escape at "+flowNodeLoopPath.getName());
                        return true;
                    }
                }
                errors.add("Livelock loop detected");
                return false;
            }

            activeFlowNode.getPath().add(activeFlowNode.getCurrentFlowNode());

        }

        boolean endCorrect = true;
        List<BpmnToken> activeFlowNodesClone = activeFlowNodes.stream().map(bpmnToken -> new BpmnToken(bpmnToken)).collect(Collectors.toList());
        for (BpmnToken activeFlowNode : activeFlowNodesClone) {
            List<FlowNode> flowNodesOutgoing = getOutgoingFlowNodes(activeFlowNode.getCurrentFlowNode());

            if (flowNodesOutgoing.size() == 0 && !(activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                errors.add("Process ends on non-EndEvent: " + activeFlowNode.getCurrentFlowNode().getName());
                // The process is invalid!

                activeFlowNodes.remove(activeFlowNode);

            } else if (activeFlowNodes.size() > 1 && (activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                errors.add("Multiple Termination detected : " + activeFlowNode.getCurrentFlowNode().getName());

                // Continue
                activeFlowNodes.remove(activeFlowNode);
                boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes);
                if (!subEndCorrect) endCorrect = false;
            } else if (activeFlowNode.getCurrentFlowNode() instanceof ExclusiveGateway || activeFlowNode.getCurrentFlowNode() instanceof EventBasedGateway) {

                // Exclusive decision
                for (FlowNode flowNodeOutgoing : flowNodesOutgoing) {
                    activeFlowNodes.remove(activeFlowNode);
                    List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                    newActiveFlowNodes.add(new BpmnToken(flowNodeOutgoing, new ArrayList<>(activeFlowNode.getPath())));
                    boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, waitingFlowNodes);
                    if (!subEndCorrect) endCorrect = false;
                }
            } else if (activeFlowNode.getCurrentFlowNode() instanceof ParallelGateway) {
                // Possible Join

                // ToDo: fix joins
                List<FlowNode> incomingFlowNodes = activeFlowNode.getCurrentFlowNode().getIncoming().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                List<BpmnToken> joinCandidates = activeFlowNodes.stream().filter(flowNode -> flowNode.getCurrentFlowNode().getId().equals(activeFlowNode.getCurrentFlowNode().getId())).collect(Collectors.toList());
                if (incomingFlowNodes.size() == joinCandidates.size()) {
                    List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                    newActiveFlowNodes.removeAll(joinCandidates);


                    newActiveFlowNodes.remove(activeFlowNode);
                    newActiveFlowNodes.addAll(flowNodesOutgoing.stream().map(flowNode -> new BpmnToken(flowNode, new ArrayList<>(activeFlowNode.getPath()))).collect(Collectors.toList()));

                    boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, waitingFlowNodes);
                    if (!subEndCorrect) endCorrect = false;
                }
                else {
                    // ToDo : Ref error
                    waitingFlowNodes.add(activeFlowNode);
                }
            } else if (!(activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                activeFlowNodes.remove(activeFlowNode);
                List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                newActiveFlowNodes.addAll(flowNodeListToTokenList(flowNodesOutgoing, activeFlowNode.getPath()));

                boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, waitingFlowNodes);
                if (!subEndCorrect) endCorrect = false;
            } else {
                activeFlowNodes.remove(activeFlowNode);
            }
        }

        if (activeFlowNodes.size() > 0) {
            errors.add("Deadlock at " + activeFlowNodes.get(0).getCurrentFlowNode().getName() + (activeFlowNodes.size() > 1 ? " and " + activeFlowNodes.size() + " more" : "") + " detected!");
            return false;
        }
        return endCorrect;
    }

}
