package service;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.ModelParseException;

import java.io.File;
import java.util.Optional;

public class BpmnModelService {
    /**
     * Loads a BPMN Model from a given path
     * @param pathname - Path of the BPMN Model
     * @return Loaded BpmnModelInstance
     */
    public Optional<BpmnModelInstance> loadDiagram(String pathname) {
        LogService.logEvent("BpmnModelService", "Try to load diagram from " + pathname);
        try {
            // Load file
            File bpmnModelFile = new File(pathname);
            BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromFile(bpmnModelFile);

            // DOM validation
            Bpmn.validateModel(bpmnModelInstance);

            LogService.logEvent("BpmnModelService", "Loaded successful");

            return Optional.of(bpmnModelInstance);
        } catch (ModelParseException saxE) {
            LogService.logEvent("BpmnModelService", "Error while loading: " + saxE.getMessage());
            return Optional.empty();
        }


    }
}
