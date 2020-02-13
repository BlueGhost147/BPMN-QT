package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.ModelInstance;
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

        List<ModelElementInstance> endEventsTerminate = endEvents.stream().filter(endEvent -> ((EndEvent) endEvent).getEventDefinitions().stream().anyMatch(eventDefinition -> eventDefinition instanceof TerminateEventDefinition)).collect(Collectors.toList());
        boolean correctEnd = checkNodePathToken(newStartList, errors, 0, new ArrayList<>(), endEventsTerminate, new TerminationToken());
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
        List<ModelElementInstance> endEventsTerminate = endEvents.stream().filter(endEvent -> ((EndEvent) endEvent).getEventDefinitions().stream().anyMatch(eventDefinition -> eventDefinition instanceof TerminateEventDefinition)).collect(Collectors.toList());

        boolean correctEnd = checkNodePathToken(newStartList, errors, 0, new ArrayList<>(), endEventsTerminate, new TerminationToken());
        if (!correctEnd)
            errors.add("Process structure error detected!");

        return errors;
    }

    /**
     * Get all the outgoing flownodes form a given flownode
     */
    private List<FlowNode> getOutgoingFlowNodes(FlowNode flowNode) {
        return flowNode.getOutgoing().stream().map(SequenceFlow::getTarget).collect(Collectors.toList());
    }


    private List<BpmnToken> moveTokenToMulitble(BpmnToken oldToken, List<FlowNode> newFlowNodes) {
        return newFlowNodes.stream().map(newFlowNode -> oldToken.moveAndCloneToken(oldToken, newFlowNode)).collect(Collectors.toList());
    }

    private boolean checkNodePathToken(List<BpmnToken> activeFlowNodes, List<String> errors, int overflowCounter, List<BpmnToken> waitingFlowNodes,
                                       List<ModelElementInstance> termEndEvents, TerminationToken terminationToken) {
        overflowCounter++;

        if(terminationToken.isTerminated()) return true;

        if (overflowCounter > 1000) {
            errors.add("Overflow counter reached, abort validation");
            return false;
        }

        // Prevent endless loops
        for (BpmnToken activeFlowNode : activeFlowNodes) {
            int position = activeFlowNode.getPath().indexOf(activeFlowNode.getCurrentFlowNode());
            if (position >= 0) {
                // Loop detected !
                boolean invalidLoop = true;
                for (int i = position; i < activeFlowNode.getPath().size(); i++) {
                    FlowNode flowNodeLoopPath = activeFlowNode.getPath().get(i);

                    if (flowNodeLoopPath instanceof ExclusiveGateway || flowNodeLoopPath instanceof EventBasedGateway) {
                        List<FlowNode> possibleLoopBreaks = getOutgoingFlowNodes(flowNodeLoopPath);
                        possibleLoopBreaks.removeAll(activeFlowNode.getVisitedNodes());

                        if (possibleLoopBreaks.size() > 0) {
                            System.out.println("Loop with possible escape at " + flowNodeLoopPath.getName());

                            invalidLoop = false;

                            for (FlowNode flowNodeBreaks : possibleLoopBreaks) {
                                activeFlowNodes.remove(activeFlowNode);
                                List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                                newActiveFlowNodes.add(activeFlowNode.moveAndCloneToken(activeFlowNode, flowNodeBreaks));
                                boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, new ArrayList<>(waitingFlowNodes), termEndEvents, new TerminationToken());
                                if (!subEndCorrect) invalidLoop = true;
                            }
                        }

                        if (activeFlowNodes.size() == 0)
                            return true;
                    }
                }

                if (invalidLoop) {
                    if (termEndEvents.size() > 0)
                        errors.add("Livelock at "+ activeFlowNode.getCurrentFlowNode().getName()+" detected - but termination could be possible");
                    else
                        errors.add("Livelock at "+ activeFlowNode.getCurrentFlowNode().getName()+" detected");

                    activeFlowNodes.clear();
                    return false;
                }
            }
        }

        boolean endCorrect = true;
        List<BpmnToken> activeFlowNodesClone = activeFlowNodes.stream().map(bpmnToken -> new BpmnToken(bpmnToken)).collect(Collectors.toList());
        for (BpmnToken activeFlowNode : activeFlowNodesClone) {

            if (activeFlowNodes.indexOf(activeFlowNode) == -1) {
                continue;
            }
            List<FlowNode> flowNodesOutgoing = getOutgoingFlowNodes(activeFlowNode.getCurrentFlowNode());

            if (flowNodesOutgoing.size() == 0 && !(activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                errors.add("Process ends on non-EndEvent: " + activeFlowNode.getCurrentFlowNode().getName());
                // The process is invalid!

                activeFlowNodes.remove(activeFlowNode);

            } else if (activeFlowNodes.size() > 1 && (activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                errors.add("Multiple Termination detected : " + activeFlowNode.getCurrentFlowNode().getName());

                // Continue
                activeFlowNodes.remove(activeFlowNode);
                //boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes, termEndEvents);
                //if (!subEndCorrect) endCorrect = false;
            } else if (activeFlowNode.getCurrentFlowNode() instanceof ExclusiveGateway || activeFlowNode.getCurrentFlowNode() instanceof EventBasedGateway) {

                // Exclusive decision
                for (FlowNode flowNodeOutgoing : flowNodesOutgoing) {
                    activeFlowNodes.remove(activeFlowNode);
                    List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                    newActiveFlowNodes.add(activeFlowNode.moveAndCloneToken(activeFlowNode, flowNodeOutgoing));
                    boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, new ArrayList<>(waitingFlowNodes), termEndEvents, new TerminationToken());
                    if (!subEndCorrect) endCorrect = false;
                }
            } else if (activeFlowNode.getCurrentFlowNode() instanceof ParallelGateway) {

                List<FlowNode> incomingFlowNodes = activeFlowNode.getCurrentFlowNode().getIncoming().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                boolean continueSF = true;

                // Check Possible Join
                if (incomingFlowNodes.size() > 1) {
                    // Join could happen

                    // Add waiting tokens and activeTokens which are joinable
                    List<BpmnToken> joinCandidates = activeFlowNodes.stream().filter(flowNode -> flowNode.getCurrentFlowNode().getId().equals(activeFlowNode.getCurrentFlowNode().getId())).collect(Collectors.toList());
                    joinCandidates.addAll(waitingFlowNodes.stream().filter(flowNode -> flowNode.getCurrentFlowNode().getId().equals(activeFlowNode.getCurrentFlowNode().getId())).collect(Collectors.toList()));

                    if (incomingFlowNodes.size() <= joinCandidates.size()) {
                        // Get all the prev FN from the token path
                        List<FlowNode> joinPred = joinCandidates.stream()
                                .map(joinCandidate -> joinCandidate.getPath().get(joinCandidate.getPath().size() - 1))
                                .collect(Collectors.toList());

                        boolean allIncMatch = incomingFlowNodes.stream().allMatch(incFN -> joinPred.indexOf(incFN) >= 0);
                        if (allIncMatch) {
                            activeFlowNodes.removeAll(joinCandidates);
                            waitingFlowNodes.removeAll(joinCandidates);
                        } else {
                            // The token has to wait, add to list for join
                            waitingFlowNodes.add(activeFlowNode);
                            continueSF = false;
                        }
                    } else {
                        // The token has to wait, add to list for join
                        waitingFlowNodes.add(activeFlowNode);
                        continueSF = false;
                    }
                }

                if (continueSF) {
                    activeFlowNodes.remove(activeFlowNode);
                    activeFlowNodes.addAll(moveTokenToMulitble(activeFlowNode, flowNodesOutgoing));

                    boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes, termEndEvents, terminationToken);
                    if (!subEndCorrect) endCorrect = false;
                }

            } else if (!(activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                activeFlowNodes.remove(activeFlowNode);
                //List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                //newActiveFlowNodes.addAll(moveTokenToMulitble(activeFlowNode, flowNodesOutgoing));
                activeFlowNodes.addAll(moveTokenToMulitble(activeFlowNode, flowNodesOutgoing));

                //boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, waitingFlowNodes);
                boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes, termEndEvents, terminationToken);
                if (!subEndCorrect) endCorrect = false;
            } else {
                EndEvent endEvent = (EndEvent) activeFlowNode.getCurrentFlowNode();
                boolean termination = endEvent.getEventDefinitions().stream().anyMatch(eventDefinition -> eventDefinition instanceof TerminateEventDefinition);
                if (termination) {
                    terminationToken.setTerminated(true);
                    activeFlowNodes.clear();
                    //errors.add("Usage of TerminateEndEvent");
                }
                activeFlowNodes.remove(activeFlowNode);
            }
        }

        if (activeFlowNodes.size() > 0) {
            errors.add("Deadlock at " + activeFlowNodes.get(0).getCurrentFlowNode().getName() + (activeFlowNodes.size() > 1 ? " and " + activeFlowNodes.size() + " more" : "") + " detected!");
            activeFlowNodes.clear();
            return false;
        }
        return endCorrect;
    }

    private class TerminationToken
    {
        private boolean terminated = false;

        public boolean isTerminated() {
            return terminated;
        }

        public void setTerminated(boolean terminated) {
            this.terminated = terminated;
        }
    }

}
