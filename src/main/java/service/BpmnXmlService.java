package service;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class BpmnXmlService {

/*
    private static final String[] xmlSchemas = new String[]{
            //"https://www.omg.org/spec/BPMN/20100501/BPMN20.xsd",
            //"https://www.omg.org/spec/BPMN/20100501/BPMNDI.xsd",
            //"https://www.omg.org/spec/BPMN/20100501/DC.xsd",
            //"https://www.omg.org/spec/BPMN/20100501/DI.xsd",
            "https://www.omg.org/spec/BPMN/20100501/Semantic.xsd"
    };*/

    private static final String xmlSchemaDefault = "https://www.omg.org/spec/BPMN/20100501/BPMN20.xsd";

    public List<String> validateXML(String xmlFilePath) {
        return validateXML(xmlFilePath, xmlSchemaDefault);
    }

    public List<String> validateXML(String xmlFilePath, String xmlSchema) {

        try {
            return validateXml(loadXmlFile(xmlFilePath), xmlSchema).stream().map(SAXException::getMessage).collect(Collectors.toList());
        } catch (IOException | SAXException e) {
            List<String> errors =  new ArrayList<>();
            errors.add("Failed to load the XML file: "+e.getMessage());
            return errors;
        }

    }


    /**
     * Validate one XML File via a given Schema
     *
     * @param xmlFile     - Filesource of the XML file to validate
     * @param xsdFilePath - Filepath of the XML schema
     * @throws MalformedURLException - URL of the Schema not valid
     * @throws SAXException          - Schema file failed to load
     */
    public List<SAXParseException> validateXml(Source xmlFile, String xsdFilePath) throws IOException, SAXException {

        Schema schema = loadXmlSchema(xsdFilePath);

        Validator validator = schema.newValidator();
        final List<SAXParseException> exceptions = new LinkedList<SAXParseException>();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                exceptions.add(exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                exceptions.add(exception);
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                exceptions.add(exception);
            }
        });

        validator.validate(xmlFile);

        return exceptions;
    }

    /**
     * Loads a XML File a given path
     *
     * @param xmlFilePath - Filepath of the XML
     * @return XML as StreamSource
     */
    private Source loadXmlFile(String xmlFilePath) {
        return new StreamSource(new File(xmlFilePath));
    }

    /**
     * Load a XML Schema via a given path
     *
     * @param schemaPath - XML Schema path
     * @return XML Schema
     * @throws MalformedURLException - URL not valid
     * @throws SAXException          - File failed to load
     */
    private Schema loadXmlSchema(String schemaPath) throws MalformedURLException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return factory.newSchema(new URL(schemaPath));
    }
}
