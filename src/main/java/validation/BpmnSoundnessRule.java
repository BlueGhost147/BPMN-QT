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

/**
 * Test the soundness of a given BPMN Model
 */
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

        // Check if pools exits in the model
        // true -> test the process of each pool
        // false -> the the whole model as one instance
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

    /**
     * Test if every element can participate in the process
     *
     * @param modelInstance - Model which should be analysed
     * @return - List of errors
     */
    private List<String> validateSoundnessCategory1(BpmnModelInstance modelInstance) {
        // Find all FlowNodes of the process
        return bpmnService.findElementsByType(modelInstance, FlowNode.class).stream()
                // Filter StartEvents since they can´t have incoming SequenceFlows
                .filter(flowNode -> !(flowNode instanceof StartEvent || flowNode instanceof BoundaryEvent))
                // Find all FlowNodes with no incoming SequenceFlows
                .filter(flowNodeNoStartEvents -> ((FlowNode) flowNodeNoStartEvents).getIncoming().size() == 0)
                // Create a ErrorMsg out of all found FlowNodes
                .map(flowNodeNoIncoming -> ("Soundness Class 1 violation: " + ((FlowNode) flowNodeNoIncoming).getName()) + " has no Incoming SequenceFlows")
                .collect(Collectors.toList());
    }

    /**
     * Test for deadlocks, livelocks and mult. termination in the given pool
     */
    private List<String> validateSoundnessCategory2(Participant pool) {
        if (pool == null || pool.getProcess() == null)
            return new ArrayList<>();

        List<ModelElementInstance> startEvents = pool.getProcess().getFlowElements().stream().filter(flowElement -> flowElement instanceof StartEvent).collect(Collectors.toList());
        List<ModelElementInstance> endEvents = pool.getProcess().getFlowElements().stream().filter(flowElement -> flowElement instanceof EndEvent).collect(Collectors.toList());

        return startValidation(startEvents, endEvents, pool.getName() + "(Pool) - ");
    }

    /**
     * Test for deadlocks, livelocks and mult. termination in the whole model (no pools)
     */
    private List<String> validateSoundnessCategory2(BpmnModelInstance modelInstance) {

        List<ModelElementInstance> startEvents = new ArrayList<>(bpmnService.findElementsByType(modelInstance, StartEvent.class));
        List<ModelElementInstance> endEvents = new ArrayList<>(bpmnService.findElementsByType(modelInstance, EndEvent.class));

        return startValidation(startEvents, endEvents, "");
    }

    /**
     * Start the validation for the given start and end events
     */
    private List<String> startValidation(List<ModelElementInstance> startEvents, List<ModelElementInstance> endEvents, String logContext) {
        List<String> errors = new ArrayList<>();

        if (startEvents.size() == 0) {
            errors.add(logContext + "The process has no start event!");
        }

        if (endEvents.size() == 0) {
            errors.add(logContext + "The process has no end event!");
        }

        // Stop validation if its not a valid process
        if (errors.size() > 0) return errors;

        // Test each StartEvent
        for (ModelElementInstance startEventMEI : startEvents) {
            StartEvent startEvent = (StartEvent) startEventMEI;
            List<BpmnToken> newStartList = new ArrayList<>();
            newStartList.add(new BpmnToken(startEvent));

            String newLogContext = logContext + startEvent.getName() + "(StartEvent) - ";
            boolean correctEnd = checkNodePathToken(newStartList, errors, 0, new ArrayList<>(),new ArrayList<>(), newLogContext);
            if (!correctEnd)
                errors.add(newLogContext + "Process structure error detected!");
        }


        return errors;
    }

    private boolean checkNodePathToken(List<BpmnToken> activeFlowNodes, List<String> errors, int overflowCounter, List<BpmnToken> waitingFlowNodes,List<BlockedPath> blockedPaths, String logContext) {
        overflowCounter++;

        if (overflowCounter > 1000) {
            errors.add(logContext + "Overflow counter reached, abort validation");
            return false;
        }

        // Prevent endless loops
        for (BpmnToken activeFlowNode : filterLoops(activeFlowNodes)) {
            int position = activeFlowNode.getPath().indexOf(activeFlowNode.getCurrentFlowNode());
            if (position >= 0) {
                // Loop detected !
                boolean invalidLoop = true;
                for (int i = position; i < activeFlowNode.getPath().size(); i++) {
                    FlowNode flowNodeLoopPath = activeFlowNode.getPath().get(i);

                    // Check if the loop has a exit condition
                    if (flowNodeLoopPath instanceof ExclusiveGateway || flowNodeLoopPath instanceof EventBasedGateway) {
                        List<FlowNode> possibleLoopBreaks = getOutgoingFlowNodes(flowNodeLoopPath);

                        if(i+1 <  activeFlowNode.getPath().size()) {
                            // Block the route which was taken
                            blockedPaths.add(new BlockedPath(flowNodeLoopPath, activeFlowNode.getPath().get(i + 1)));
                            invalidLoop = false;
                        }
                        else{
                            List<FlowNode> flowNodes = getOutgoingFlowNodes(activeFlowNode.getCurrentFlowNode());
                            for (FlowNode f: flowNodes) {
                                // Block loops to itself
                                if(f == activeFlowNode.getCurrentFlowNode()) blockedPaths.add(new BlockedPath(activeFlowNode.getCurrentFlowNode(), f));
                                invalidLoop = false;
                            }
                        }

                        /*
                        possibleLoopBreaks.removeAll(activeFlowNode.getVisitedNodes());

                        if (possibleLoopBreaks.size() > 0) {
                            invalidLoop = false;

                            for (FlowNode flowNodeBreaks : possibleLoopBreaks) {

                                activeFlowNodes.remove(activeFlowNode);
                                List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                                newActiveFlowNodes.add(activeFlowNode.moveAndCloneToken(activeFlowNode, flowNodeBreaks));
                                boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, new ArrayList<>(waitingFlowNodes), logContext);
                                if (!subEndCorrect) invalidLoop = true;


                            }
                        }

                        if (activeFlowNodes.size() == 0)
                            return true;
                            */
                    }
                }

                if (invalidLoop) {
                    //if (termEndEvents.size() > 0)
                    //     errors.add("X Livelock at "+ activeFlowNode.getCurrentFlowNode().getName()+" detected - but termination could be possible");
                    // else
                    //     errors.add("X Livelock at "+ activeFlowNode.getCurrentFlowNode().getName()+" detected");

                    activeFlowNode.setLoop(true);
                    //activeFlowNodes.clear();
                    //return false;
                }
            }
        }

        boolean endCorrect = true;
        List<BpmnToken> activeFlowNodesClone = activeFlowNodes.stream().map(bpmnToken -> new BpmnToken(bpmnToken)).collect(Collectors.toList());
        // Start the movement of every currently active token
        for (BpmnToken activeFlowNode : filterLoops(activeFlowNodesClone)) {

            if (activeFlowNodes.indexOf(activeFlowNode) == -1) {
                continue;
            }
            List<FlowNode> flowNodesOutgoing = getOutgoingFlowNodes(activeFlowNode.getCurrentFlowNode());

            if (flowNodesOutgoing.size() == 0 && !(activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                errors.add(logContext + "Process ends on non-EndEvent: " + flowNodeToString(activeFlowNode.getCurrentFlowNode()));
                // The process is invalid!

                activeFlowNodes.remove(activeFlowNode);

            } else if (activeFlowNodes.size() > 1 && (activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {


                EndEvent endEvent = (EndEvent) activeFlowNode.getCurrentFlowNode();
                boolean termination = endEvent.getEventDefinitions().stream().anyMatch(eventDefinition -> eventDefinition instanceof TerminateEventDefinition);
                if (termination) {
                    errors.add(logContext + "Termination End Event: " + flowNodeToString(activeFlowNode.getCurrentFlowNode()));
                    //terminationToken.setTerminated(true);
                    activeFlowNodes.clear();
                    //errors.add("Usage of TerminateEndEvent");
                } else {
                    errors.add(logContext + "Multiple Termination detected : " + flowNodeToString(activeFlowNode.getCurrentFlowNode()));
                }

                // Continue
                activeFlowNodes.remove(activeFlowNode);
                //boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes, termEndEvents);
                //if (!subEndCorrect) endCorrect = false;
            } else if (activeFlowNode.getCurrentFlowNode() instanceof ExclusiveGateway || activeFlowNode.getCurrentFlowNode() instanceof EventBasedGateway) {

                List<FlowNode> blockedOutgoing = blockedPaths.stream()
                        .filter(blockedPath -> blockedPath.getFlowNode() == activeFlowNode.getCurrentFlowNode())
                        .map(BlockedPath::getBlockedSequenceFlow)
                        .collect(Collectors.toList());

                List<FlowNode> nonBlockedOutgoing = new ArrayList<>(flowNodesOutgoing);
                nonBlockedOutgoing.removeAll(blockedOutgoing);

                // Exclusive decision
                for (FlowNode flowNodeOutgoing : nonBlockedOutgoing) {
                    activeFlowNodes.remove(activeFlowNode);
                    List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                    newActiveFlowNodes.add(activeFlowNode.moveAndCloneToken(activeFlowNode, flowNodeOutgoing));
                    boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, new ArrayList<>(waitingFlowNodes),new ArrayList<>(blockedPaths), logContext);
                    if (!subEndCorrect) endCorrect = false;
                }
                // Decision taken
                activeFlowNodes.clear();
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

                    boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes,blockedPaths, logContext);
                    if (!subEndCorrect) endCorrect = false;
                }

            } else if (!(activeFlowNode.getCurrentFlowNode() instanceof EndEvent)) {
                activeFlowNodes.remove(activeFlowNode);
                //List<BpmnToken> newActiveFlowNodes = new ArrayList<>(activeFlowNodes);
                //newActiveFlowNodes.addAll(moveTokenToMulitble(activeFlowNode, flowNodesOutgoing));
                activeFlowNodes.addAll(moveTokenToMulitble(activeFlowNode, flowNodesOutgoing));

                //boolean subEndCorrect = checkNodePathToken(newActiveFlowNodes, errors, overflowCounter, waitingFlowNodes);
                boolean subEndCorrect = checkNodePathToken(activeFlowNodes, errors, overflowCounter, waitingFlowNodes,blockedPaths, logContext);
                if (!subEndCorrect) endCorrect = false;
            } else {
                EndEvent endEvent = (EndEvent) activeFlowNode.getCurrentFlowNode();
                boolean termination = endEvent.getEventDefinitions().stream().anyMatch(eventDefinition -> eventDefinition instanceof TerminateEventDefinition);
                if (termination) {
                    //terminationToken.setTerminated(true);
                    activeFlowNodes.clear();
                    //errors.add("Usage of TerminateEndEvent");
                }
                activeFlowNodes.remove(activeFlowNode);
            }
        }

        if (filterLoops(activeFlowNodes).size() > 0) {
            errors.add(logContext + "Deadlock at " + flowNodeToString(activeFlowNodes.get(0).getCurrentFlowNode()) + (activeFlowNodes.size() > 1 ? " and " + activeFlowNodes.size() + " more" : "") + " detected!");
            activeFlowNodes.clear();
            return false;
        }
        if (filterNonLoops(activeFlowNodes).size() > 0) {
            errors.add(logContext + "LiveLock at " + flowNodeToString(activeFlowNodes.get(0).getCurrentFlowNode()) + (activeFlowNodes.size() > 1 ? " and " + activeFlowNodes.size() + " more" : "") + " detected!");
            activeFlowNodes.clear();
            return false;
        }
        return endCorrect;
    }

    private String flowNodeToString(FlowNode flowNode)
    {
        if(flowNode == null) return "flowNode";
        return flowNode.getName()+"("+flowNode.getClass().getSimpleName()+")";
    }

    /**
     * Get all the outgoing flownodes form a given flownode
     */
    private List<FlowNode> getOutgoingFlowNodes(FlowNode flowNode) {
        return flowNode.getOutgoing().stream().map(SequenceFlow::getTarget).collect(Collectors.toList());
    }

    /**
     * Perform a parallel split from one token to mult. targets
     */
    private List<BpmnToken> moveTokenToMulitble(BpmnToken oldToken, List<FlowNode> newFlowNodes) {
        return newFlowNodes.stream().map(newFlowNode -> oldToken.moveAndCloneToken(oldToken, newFlowNode)).collect(Collectors.toList());
    }

    /**
     * Only return non-loop tokens
     */
    private List<BpmnToken> filterLoops(List<BpmnToken> tokens) {
        return tokens.stream().filter(token -> !(token.isLoop())).collect(Collectors.toList());
    }

    /**
     * Only return loop tokens
     */
    private List<BpmnToken> filterNonLoops(List<BpmnToken> tokens) {
        return tokens.stream().filter(BpmnToken::isLoop).collect(Collectors.toList());
    }

    private class BlockedPath
    {
        private FlowNode flowNode;
        private FlowNode blockedPath;

        public BlockedPath(FlowNode flowNode, FlowNode blockedPath) {
            this.flowNode = flowNode;
            this.blockedPath = blockedPath;
        }

        public FlowNode getFlowNode() {
            return flowNode;
        }

        public FlowNode getBlockedSequenceFlow() {
            return blockedPath;
        }
    }
}
