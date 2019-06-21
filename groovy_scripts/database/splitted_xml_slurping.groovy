// Example of using StAX to split a large XML document and parse a single element using XmlSlurper

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stax.StAXSource

def url = new URL("http://repo2.maven.org/maven2/archetype-catalog.xml")
url.withInputStream { inputStream ->
    def xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream)
    def transformer = TransformerFactory.newInstance().newTransformer()
    while (xmlStreamReader.hasNext()) {
        xmlStreamReader.next()
        if (xmlStreamReader.isStartElement() && xmlStreamReader.getLocalName() == 'archetype') {
            // Example of splitting a large XML document and parsing a single element with XmlSlurper at a time
            def xmlSlurper = new XmlSlurper()
            transformer.transform(new StAXSource(xmlStreamReader), new SAXResult(xmlSlurper))
            def archetype = xmlSlurper.document
            println "${archetype.groupId} ${archetype.artifactId} ${archetype.version}"
        }
    }
}
