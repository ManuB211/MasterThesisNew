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

	Document doc;

	public ChoreographyModelToCPN(List<PrivateModel> pPrivateModels, String pFormattedDate, File pOutputFolder) {
		this.formattedDate = pFormattedDate;
		this.privateModels = pPrivateModels;
		this.outputFolder = pOutputFolder;

		this.setupDocument();

	}

	/**
	 * Sets up the basic structure of the document, meaning the xml, pnml and net
	 * tags
	 */
	private void setupDocument() {
		this.doc = new Document();

		Element pnml = new Element("pnml");
		Element net = new Element("net");
		net.setAttribute("type", "http://www.yasper.org/specs/epnml-1.1");
		net.setAttribute("id", "CPN1"); // TODO: do i need something dynamic for the ID?

		Element place = createPlace("p1", 530, 80);

		net.addContent(place);
		pnml.setContent(net);
		doc.setRootElement(pnml);

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
		XMLOutputter xmlOutput = new XMLOutputter();

		// Pretty Print
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(outputFolder + "/CPN.pnml"));
	}

	/**
	 * ===================================================TAG-CREATION========================================================================
	 */

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
	 * -----------------------------------------------Transition-Element---------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------Place-Element------------------------------------------------------------------------
	 */

	/**
	 * Creates a place element
	 * 
	 * @param id:   The id of the place (will also be the name)
	 * @param posX: The x-coord of the position
	 * @param posY: The y-coord of the position
	 * 
	 * @return a <place>-element
	 */

	private Element createPlace(String id, Integer posX, Integer posY) {
		Element placeElem = new Element("place");

		placeElem.setAttribute("id", id);

		placeElem.addContent(getGraphicsElement(PNMLElementEnum.PLACE, posX, posY));
		placeElem.addContent(getNameElement(id));

		return placeElem;
	}

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
