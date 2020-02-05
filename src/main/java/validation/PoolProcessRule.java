package validation;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import service.BpmnService;

import java.util.*;
import java.util.stream.Collectors;

public class PoolProcessRule extends BpmnRule {

    private BpmnService bpmnService = null;

    public PoolProcessRule() {
        this.bpmnService = new BpmnService();
    }

    @Override
    public ValidationResult validate(BpmnModelInstance modelInstance) {
        Collection<ModelElementInstance> pools = bpmnService.findElementsByType(modelInstance, Participant.class);

        List<String> errors = new ArrayList<>();
        pools.forEach(pool -> {
            // List of all the flow elements of the pool
            Process process = ((Participant) pool).getProcess();

            // If the process is null, the pool is collapsed
            if (process == null) return;

            Collection<FlowElement> flowElements = process.getFlowElements();
            long startEventCount = flowElements.stream().filter(flowElement -> flowElement instanceof StartEvent).count();
            long endEventCount = flowElements.stream().filter(flowElement -> flowElement instanceof EndEvent).count();

            if (startEventCount == 0 && endEventCount == 0)
                errors.add("The pool " + ((Participant) pool).getName() + " has a process with no start and end events");
            if (startEventCount == 0)
                errors.add("The pool " + ((Participant) pool).getName() + " has a process with no start events");
            if (endEventCount == 0)
                errors.add("The pool " + ((Participant) pool).getName() + " has a process with no end events");
        });
        return new ValidationResult(errors.size() == 0, errors);
    }

}
