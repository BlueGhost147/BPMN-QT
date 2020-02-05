package service;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.util.ArrayList;
import java.util.Collection;

public class BpmnService {

    /**
     * @param elementClass - Search Class Type
     * @return Collection of all found ModelElementInstances
     */
    public Collection<ModelElementInstance> findElementsByType(BpmnModelInstance bpmnModel, Class<? extends ModelElementInstance> elementClass) {
        if (bpmnModel == null || elementClass == null) {
            LogService.logEvent("BpmnService" ,"findElementByType - Search called with invalid parameters!");
            return new ArrayList<>();
        }
        ModelElementType searchType = bpmnModel.getModel().getType(elementClass);
        Collection<ModelElementInstance> elementInstances = bpmnModel.getModelElementsByType(searchType);
        return elementInstances;
    }


}
