package org.matsim.project;

import scala.xml.XML;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

public class XmlParser {

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
        String path = "C:\\Users\\gusta\\Documents\\Exjobb\\Workspaces\\matsim-example-project\\test\\input\\org\\matsim\\evDetour\\1pctNetwork.xml";

        ArrayList<StartElement> links = XmlParser.findTagsInXMLFile(path, "link");
        String[] linkIDs = new String[links.size()];
        for(int i = 0; i < links.size(); i++){
            linkIDs[i] = XmlParser.getValueFromTag(links.get(i), "id");
            System.out.println(linkIDs[i]);
        }

        Random rand = new Random();
        int workIdIndex = 0;
        int homeIdIndex = 0;
        while(workIdIndex == homeIdIndex){
            workIdIndex = rand.nextInt(linkIDs.length);
            homeIdIndex = rand.nextInt(linkIDs.length);
        }
        String newWorkId = linkIDs[workIdIndex];
        String newHomeId = linkIDs[homeIdIndex];

        Map<String, Map<Attribute, Map<String, String>>> tagMap = new HashMap<>();
        Map<Attribute, Map<String, String>> condMap = new HashMap<>();

        XMLEventFactory eventFactory = XMLEventFactory.newInstance();

        Attribute workAt = eventFactory.createAttribute("type", "work");
        Attribute homeAt = eventFactory.createAttribute("type", "home");

        condMap.put(workAt, Map.of("link", newWorkId));
        condMap.put(workAt, Map.of("link", newHomeId));

        tagMap.put("activity", condMap);

        String plansPath = "C:\\Users\\gusta\\Documents\\Exjobb\\Workspaces\\matsim-example-project\\test\\input\\org\\matsim\\evDetour\\triple-charger-plan.xml";

        updateXMLFileWithCondition(plansPath, tagMap);

    }

    public static ArrayList<StartElement> findTagsInXMLFile(String filePath, String tagName) throws FileNotFoundException, XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(filePath));

        ArrayList<StartElement> foundTags = new ArrayList<>();
        while(reader.hasNext()){
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                if(startElement.getName().getLocalPart().equals(tagName)){
                    foundTags.add(startElement);
                }
            }
        }
        return foundTags;
    }

    public static String getValueFromTag(StartElement tag, String valName){
        Iterator<Attribute> attributes = tag.getAttributes();
        while(attributes.hasNext()){
            Attribute at = attributes.next();
            if(at.getName().getLocalPart().equals(valName)){
                return at.getValue();
            }

        }
        return "not found";

    }

    public static void updateXMLFile(String filepath, Map<String, Map<String,String>> elemMap) throws FileNotFoundException, XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(filepath));
        XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(new FileOutputStream(filepath));

        while(reader.hasNext()){
            XMLEvent nextEvent = reader.nextEvent();
            if(nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                String startElemName = startElement.getName().getLocalPart();
                if (elemMap.containsKey(startElemName)) {
                    Iterator<Attribute> attributes = startElement.getAttributes();
                    Map<String,String> atMap = elemMap.get(startElemName);
                    writer.add(modifyAndReturnStartElement(startElement, atMap));
                } else{
                    writer.add(startElement);
                }
            } else {
                writer.add(nextEvent);
            }
        }

        reader.close();
        writer.close();

    }

    /**
     *
     * @param filepath The path to the XML file that is to be edited.
     * @param elemMap Should be a map of maps, where the outer map holds the name of the startElement(s) that should be changed.
     *                The inner map should have identifier attributes as key (e.g. id) and a Map&lt;String,String&gt;
     *                as the value, created so that key: attribute name, value: the new value.
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public static void updateXMLFileWithCondition(String filepath, Map<String, Map<Attribute, Map<String,String>>> elemMap) throws FileNotFoundException, XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(filepath));
        XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(new FileOutputStream(filepath));

        while(reader.hasNext()){
            XMLEvent nextEvent = reader.nextEvent();
            if(nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                String startElemName = startElement.getName().getLocalPart();
                if (elemMap.containsKey(startElemName)) {
                    Map<Attribute, Map<String,String>> atMap = elemMap.get(startElemName);
                    Iterator<Attribute> attributes = startElement.getAttributes();
                    boolean startElemChanged = false;
                    while (attributes.hasNext()){
                        Attribute at = attributes.next();
                        if(atMap.containsKey(at)){
                            writer.add(modifyAndReturnStartElement(startElement, atMap.get(at)));
                            startElemChanged = true;
                            break;
                        }
                    }
                    if(!startElemChanged){
                        writer.add(startElement);
                    }
                } else{
                    writer.add(startElement);
                }
            } else {
                writer.add(nextEvent);
            }
        }

        reader.close();
        writer.close();

    }

    private static StartElement modifyAndReturnStartElement(StartElement startElement, Map<String,String> atMap){

        Iterator<Attribute> attributes = startElement.getAttributes();
        ArrayList<Attribute> updatedAttributes = new ArrayList<>();

        XMLEventFactory eventFactory = XMLEventFactory.newInstance();

        while(attributes.hasNext()){
            Attribute at = attributes.next();
            String atName = at.getName().getLocalPart();
            if(atMap.containsKey(atName)){
                updatedAttributes.add(eventFactory.createAttribute(atName, atMap.get(atName)));
            } else {
                updatedAttributes.add(at);
            }
        }

        return eventFactory.createStartElement(startElement.getName(), updatedAttributes.iterator(), startElement.getNamespaces());
    }
}
