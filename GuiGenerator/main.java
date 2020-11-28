package myMiniGUIGenarator;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class main {

	private static DOMUIParser guiParser;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			File xmlfile = new File("input/4.xml");
			Document doc = builder.parse(xmlfile);
			doc.setDocumentURI("input/4.xml");
			guiParser = new DOMUIParser();
			guiParser.buildUI(doc);
		}catch (Exception e){
			System.err.println(e.getMessage());
		}

	}

}
