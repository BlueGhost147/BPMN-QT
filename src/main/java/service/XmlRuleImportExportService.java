package service;

import validation.BpmnRule;
import validation.RuleList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Source: https://howtodoinjava.com/jaxb/write-object-to-xml/
 */
public class XmlRuleImportExportService {

    public static void saveRulesToXML(RuleList ruleList, String path) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(RuleList.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            File file = new File(path); // "bpmnRule.qtrules"
            jaxbMarshaller.marshal(ruleList, file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static RuleList loadRulesFromXML(String fileName) {
        File xmlFile = new File(fileName);
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(RuleList.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (RuleList) jaxbUnmarshaller.unmarshal(xmlFile);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return new RuleList();
    }


}
