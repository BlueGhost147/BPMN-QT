package service;

import configuration.BpmnQtConfig;
import javafx.scene.control.TextArea;


public class LogService {

    public static TextArea textArea;

    public static void logEvent(String source, String logString) {
        String logMessage = "[" + source + "] - " + logString;
        log(logMessage);
    }

    public static void logLine(String source) {
        log("--------------------------------------");
    }

    /**
     * Log the message the console and to the given textArea
     *
     * @param logMessage - Message which should be logged
     */
    private static void log(String logMessage) {
        if (BpmnQtConfig.logging) {
            if (textArea != null) {
                javafx.application.Platform.runLater( () -> textArea.appendText(logMessage + "\n") );
                //textArea.appendText(logMessage + "\n");
            }
            System.out.println(logMessage);
        }
    }

}
