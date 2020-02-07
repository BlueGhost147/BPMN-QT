package xmlexport;

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
public class XmlRuleService {

    public static void jaxbObjectToXML(RuleList ruleList, String path)
    {
        try
        {
            //Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(RuleList.class);

            //Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            //Required formatting??
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            //Store XML to File
            File file = new File(path); // "bpmnRule.qtrules"

            //Writes XML file to file-system
            jaxbMarshaller.marshal(ruleList, file);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }
    }

    public static RuleList jaxbXmlFileToObject(String fileName) {

        File xmlFile = new File(fileName);

        JAXBContext jaxbContext;
        try
        {
            jaxbContext = JAXBContext.newInstance(RuleList.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            RuleList ruleList = (RuleList) jaxbUnmarshaller.unmarshal(xmlFile);

            return ruleList;
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }
        return new RuleList();
    }


}
