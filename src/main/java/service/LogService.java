package service;

import configuration.BpmnQtConfig;

public class LogService {
    public static void logEvent(String source, String logMessage) {
        if (BpmnQtConfig.logging)
            System.out.println("[" + source + "] -" + logMessage);
    }
}
