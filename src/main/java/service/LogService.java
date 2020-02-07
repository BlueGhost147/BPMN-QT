package service;

import configuration.BpmnQtConfig;
import javafx.scene.control.TextArea;


public class LogService {

    public TextArea textArea;

    public LogService() {

    }

    public LogService(TextArea textArea) {
        this.textArea = textArea;
    }

    public void logEvent(String source, String logString) {
        //String logMessage = "[" + source + "] - " + logString;
        log(logString);
    }

    public void logLine(String source) {
        log("--------------------------------------");
    }

    /**
     * Log the message the console and to the given textArea
     *
     * @param logMessage - Message which should be logged
     */
    private void log(String logMessage) {
        if (BpmnQtConfig.logging) {
            if (textArea != null) {
                javafx.application.Platform.runLater( () -> textArea.appendText(logMessage + "\n") );
                //textArea.appendText(logMessage + "\n");
            }
            System.out.println(logMessage);
        }
    }

}
