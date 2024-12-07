package at.ac.c3pro.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import at.ac.c3pro.chormodel.PrivateModel;

public class ChoreographyModelToCPN {

	private enum PNMLElementEnum {
		PLACE, TRANSITION
	}

	private final Integer DIMENSION_PLACE = 20;
	private final Integer DIMENSION_TRANSITION = 32;

	private String formattedDate;
	private List<PrivateModel> privateModels;

	private File outputFolder;
	
	//The parent element of all the petri net tags
	private Element net;

	Document doc;

	public ChoreographyModelToCPN(List<PrivateModel> pPrivateModels, String pFormattedDate, File pOutputFolder) {
		this.formattedDate = pFormattedDate;
		this.privateModels = pPrivateModels;
		this.outputFolder = pOutputFolder;
		
		this.net = new Element("net");
		this.net.setAttribute("type", "http://www.yasper.org/specs/epnml-1.1");
		this.net.setAttribute("id","CPN1"); // TODO: do i need something dynamic for the ID?
		this.setupDocument();

	}

	/**
	 * Sets up the basic structure of the document, meaning the xml, pnml and net
	 * tags
	 */
	private void setupDocument() {
		
		createPlace("p1", 530, 80);
		createPlace("p2", 530, 380);
		createTransition("tr1", 530, 230);
		createArc("a1", "p1", "tr1");
		createArc("a2", "tr1", "p2");

	}

	/**
	 * Builds the PNML model for a single participant
	 * 
	 * @param participantModel: the private model of the participant to build the
	 *                          PNML of
	 */
	private void buildPNMLForSingleParticipant(PrivateModel participantModel) {

	}

	/**
	 * Prints the CPN.xml file to the output folder
	 */
	public void printXML() throws IOException {
		
		Document doc = new Document();
		Element pnml = new Element("pnml");
		
		pnml.setContent(net);
		doc.setRootElement(pnml);
		
		
		XMLOutputter xmlOutput = new XMLOutputter();

		// Pretty Print
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(outputFolder + "/CPN.pnml"));
	}

	/**
	 * ===================================================TAG-CREATION========================================================================
	 */
	
	/**
	 * Creates an arc given the source and target element
	 * 
	 * @param id: The id of the arc
	 * @param source: The source of the arc
	 * @param target: The target of the arc
	 * 
	 * @return an <arc>-tag
	 * */
	private void createArc(String id, String sourceId, String targetId) {
		Element arcElem = new Element("arc");
		arcElem.setAttribute("id", id);
		
		arcElem.setAttribute("source", sourceId);
		arcElem.setAttribute("target", targetId);
		
		net.addContent(arcElem);
	}
	
	/**
	 * Creates a transition element with automatic dimensions (normal transition)
	 * 
	 * @param id: the id of the element
	 * @param posX: the x-coordinate of the element
	 * @param posY: the y-coordinate of the element
	 * 
	 * @return a <transition>-tag
	 * */
	private void createTransition(String id, Integer posX, Integer posY) {
		Element transitionElem = new Element("transition");
		
		transitionElem.setAttribute("id", id);
		transitionElem.addContent(getGraphicsElement(PNMLElementEnum.TRANSITION, posX, posY));
		
		net.addContent(transitionElem);
	}
	
	/**
	 * Creates a transition element with given dimensions (synchronous task)
	 * 
	 * @param id: the id of the element
	 * @param posX: the x-coordinate of the element
	 * @param posY: the y-coordinate of the element
	 * @param dimX: the width
	 * @param dimY: the height
	 * 
	 * @return a <transition>-tag
	 * */
	private void createTransition(String id, Integer posX, Integer posY, Integer dimX, Integer dimY) {
		Element transitionElem = new Element("transition");
		
		transitionElem.setAttribute("id", id);
		transitionElem.addContent(getGraphicsElement(posX, posY, dimX, dimY));
		
		net.addContent(transitionElem);
	}
	
	
	/**
	 * Creates a place element
	 * 
	 * @param id:   The id of the place (will also be the name)
	 * @param posX: The x-coord of the position
	 * @param posY: The y-coord of the position
	 * 
	 * @return a <place>-element
	 */

	private void createPlace(String id, Integer posX, Integer posY) {
		Element placeElem = new Element("place");

		placeElem.setAttribute("id", id);

		placeElem.addContent(getGraphicsElement(PNMLElementEnum.PLACE, posX, posY));
		placeElem.addContent(getNameElement(id));

		net.addContent(placeElem);
	}

	/**
	 * Gets a graphics-tag containing the dimension and position information for a
	 * place or transition
	 * 
	 * @param type: the type of the element for the graphics tag
	 * @param posX: x-coordinate of the Position
	 * @param posy: Y-coordinate of the Position
	 * 
	 * @return a <graphics>-element
	 */
	private Element getGraphicsElement(PNMLElementEnum type, Integer posX, Integer posY) {

		if (PNMLElementEnum.PLACE.equals(type)) {
			return getGraphicsElement(posX, posY, DIMENSION_PLACE, DIMENSION_PLACE);
		} else if (PNMLElementEnum.TRANSITION.equals(type)) {
			return getGraphicsElement(posX, posY, DIMENSION_TRANSITION, DIMENSION_TRANSITION);
		} else {
			return getGraphicsElement(posX, posY, -1, -1);
		}

	}

	
	/**
	 * Gets a graphics-tag containing the dimension and position information for a
	 * place or transition
	 * 
	 * @param posX: x-coordinate of the Position
	 * @param posY: Y-coordinate of the Position
	 * @param dimX: width of the element
	 * @param dimY: height of the element
	 * 
	 * @return a <graphics>-element
	 */
	private Element getGraphicsElement(Integer posX, Integer posY, Integer dimX, Integer dimY) {

		Element graphicsElem = new Element("graphics");

		Element posElem = getPositionElement(posX, posY);
		Element dimElem = getDimensionElement(dimX, dimY);

		graphicsElem.addContent(posElem);
		graphicsElem.addContent(dimElem);

		return graphicsElem;
	}

	/**
	 * Gets a position-tag
	 * 
	 * @param x: The x coordinate
	 * @param y: The y coordinate
	 * 
	 * @return: a <position>-Element
	 */
	private Element getPositionElement(Integer x, Integer y) {
		Element posElem = new Element("position");
		posElem.setAttribute("x", x.toString());
		posElem.setAttribute("y", y.toString());

		return posElem;
	}

	/**
	 * Gets a dimension-element based on the given values
	 * 
	 * @param x: The width
	 * @param y: The height
	 * 
	 * @return a <dimension>-element with the respective height
	 */
	private Element getDimensionElement(Integer x, Integer y) {

		if (x < 0 || y < 0) {
			throw new IllegalStateException(
					"The creation of the dimension element failed, because neither input dimensions are positive, nor was a valid element-type given from which it could have been deducted");
		}

		Element dimElem = new Element("dimension");
		dimElem.setAttribute("x", x.toString());
		dimElem.setAttribute("y", y.toString());

		return dimElem;

	}

	/**
	 * Gets a name-tag
	 * 
	 * @param name: the name to be given
	 * 
	 * @return: a <name>-element
	 */
	private Element getNameElement(String name) {
		Element nameElem = new Element("name");

		Element textElem = new Element("text");
		textElem.addContent(name);

		nameElem.addContent(textElem);

		return nameElem;

	}

}
