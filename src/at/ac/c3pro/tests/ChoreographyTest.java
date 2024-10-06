package at.ac.c3pro.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;

import at.ac.c3pro.ChangeNegotiation.NegotiationSimulation;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.ChoreographyModel;
import at.ac.c3pro.chormodel.Collaboration;
import at.ac.c3pro.chormodel.IPrivateModel;
import at.ac.c3pro.chormodel.MultiDirectedGraph;
import at.ac.c3pro.chormodel.PrivateModel;
import at.ac.c3pro.chormodel.PublicModel;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.chormodel.RpstModel;
import at.ac.c3pro.node.AndGateway;
import at.ac.c3pro.node.Edge;
import at.ac.c3pro.node.Event;
import at.ac.c3pro.node.Gateway;
import at.ac.c3pro.node.IChoreographyNode;
import at.ac.c3pro.node.INode;
import at.ac.c3pro.node.IPrivateNode;
import at.ac.c3pro.node.IPublicNode;
import at.ac.c3pro.node.Interaction;
import at.ac.c3pro.node.Message;
import at.ac.c3pro.node.PrivateActivity;
import at.ac.c3pro.node.Receive;
import at.ac.c3pro.node.Send;
import at.ac.c3pro.node.XorGateway;
import at.ac.c3pro.util.FragmentUtil;

public class ChoreographyTest {

	// RPST of choreography model
	private RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTrip;
	private RpstModel<Edge<IPublicNode>, IPublicNode> pum_acquirer;
	private RpstModel<Edge<IPrivateNode>, IPrivateNode> prm_acquirer;
	private RpstModel<Edge<IPublicNode>, IPublicNode> pum_travelAgency;
	private RpstModel<Edge<IPublicNode>, IPublicNode> pum_airline;
	private RpstModel<Edge<IPublicNode>, IPublicNode> pum_traveler;
	private Choreography bookTripChoreography;
	private Collaboration collab;

	private Role traveler, acquirer, travelAgency, airline;
	private Message m1, m2, m3, m4, m5, m6, m7, m8, m9;

	// choreography elements
	private IChoreographyNode e1, e2, i1, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11;
	private Gateway g1, g2, g3, g4, g5, g6, g7, g8;

	// public model elements
	private Receive r11;
	private Gateway g11, g12, g13, g14, g15, g16;
	private IPublicNode s11, s12, s13, s14;
	private Event e11, e12;

	// private model elements
	private IPrivateNode pr11, ps11, ps12, ps13, ps14;
	private Gateway pg11, pg12, pg13, pg14, pg15, pg16;
	private Event pe11, pe12;
	private IPrivateNode pa11, pa12, pa13;

	// TA public model elements
	private Receive ta3;
	private Send ta4;

	private void declareCommon() {
		// Roles
		this.traveler = new Role("Traveler");
		this.travelAgency = new Role("TravelAgency");
		this.acquirer = new Role("TAcquirer");
		this.airline = new Role("Airline");

		// Messages
		this.m1 = new Message("book_trip");
		this.m2 = new Message("check_cash");
		this.m3 = new Message("TA_failure");
		this.m4 = new Message("credit_carNA");
		this.m5 = new Message("A_failure");
		this.m6 = new Message("payment_ok");
		this.m7 = new Message("e-ticket");
		this.m8 = new Message("post_ticket");
		this.m9 = new Message("approval");

	}

	private void buildChoreographyModel() {
		/***** ChoreographyModel *******/
		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> Choreo = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
		this.e1 = new Event("start");
		this.e2 = new Event("end");
		this.i1 = new Interaction("I1", traveler, travelAgency, this.m1);
		this.i2 = new Interaction("I2", travelAgency, acquirer, this.m2);
		this.i3 = new Interaction("I3", acquirer, travelAgency, this.m3);
		this.i4 = new Interaction("I4", travelAgency, traveler, this.m4);
		this.i5 = new Interaction("I5", acquirer, airline, this.m5);
		this.i6 = new Interaction("I6", acquirer, airline, this.m6);
		this.i7 = new Interaction("I7", airline, travelAgency, this.m7);
		this.i8 = new Interaction("I8", airline, travelAgency, this.m8);
		this.i9 = new Interaction("I9", acquirer, travelAgency, this.m9);
		this.i10 = new Interaction("I10", acquirer, travelAgency, this.m9);
		this.i11 = new Interaction("I11", acquirer, travelAgency, this.m9);
		this.g1 = new XorGateway("g1");
		this.g2 = new XorGateway("g2");
		this.g3 = new XorGateway("g3");
		this.g4 = new AndGateway("g4");
		this.g5 = new XorGateway("g5");
		this.g6 = new XorGateway("g6");
		this.g7 = new AndGateway("g7");
		this.g8 = new XorGateway("g8");

		Choreo.addEdge(e1, i1);
		Choreo.addEdge(i1, i10);
		Choreo.addEdge(i10, i11);
		Choreo.addEdge(i11, i2);
		Choreo.addEdge(i2, g1);
		Choreo.addEdge(g1, g2);
		Choreo.addEdge(g1, g4);
		Choreo.addEdge(g2, i3);
		Choreo.addEdge(i3, i4);
		Choreo.addEdge(g2, i5);
		Choreo.addEdge(i5, g3);
		Choreo.addEdge(i4, g3);
		Choreo.addEdge(g4, i6);
		Choreo.addEdge(g4, i9);
		Choreo.addEdge(i6, g5);
		Choreo.addEdge(g5, i7);
		Choreo.addEdge(g5, i8);
		Choreo.addEdge(i8, g6);
		Choreo.addEdge(i7, g6);
		Choreo.addEdge(g6, g7);
		Choreo.addEdge(i9, g7);
		Choreo.addEdge(g7, g8);
		Choreo.addEdge(g3, g8);
		Choreo.addEdge(g8, e2);

		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTrip = new ChoreographyModel(Choreo);
		IOUtils.toFile("BookTripModel.dot", BookTrip.getdigraph().toDOT());
		IOUtils.toFile("BookTripRpst.dot", BookTrip.toDOT());

		// System.out.println(g2.isJoin(BookTrip));
		this.BookTrip = BookTrip;
		// System.out.println("-------------------"+this.BookTrip.getChildren(this.BookTrip.getRoot()));
	}

	private void buildPublicAcquirerModel() {
		/************ Public Model of the Acquirer ******************************/
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> PuM1 = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();

		this.r11 = new Receive(travelAgency, this.m2, "receive_TA_check");
		this.s12 = new Send(travelAgency, m3, "send_TA_failure");
		this.s11 = new Send(airline, m5, "send_A_failure");
		this.s13 = new Send(airline, m6, "send_A_payment");
		this.s14 = new Send(travelAgency, m9, "send_TA_approval");
		this.g11 = new XorGateway("g11");
		this.g13 = new XorGateway("g13");
		this.g12 = new XorGateway("g12");
		this.g14 = new AndGateway("g14");
		this.g15 = new AndGateway("g15");
		this.g16 = new XorGateway("g16");
		this.e11 = new Event("start");
		this.e12 = new Event("end");

		Receive r11copy = (Receive) this.r11.clone();

		// System.out.println(r11.Complement(r11.role));
		PuM1.addEdge(e11, r11);
		PuM1.addEdge(r11, g11);
		PuM1.addEdge(g11, g12);
		PuM1.addEdge(g11, g14);
		PuM1.addEdge(g12, s11);
		PuM1.addEdge(s11, g13);
		PuM1.addEdge(g12, s12);
		PuM1.addEdge(s12, g13);
		PuM1.addEdge(g13, g16);
		PuM1.addEdge(g14, s13);
		PuM1.addEdge(s13, g15);
		PuM1.addEdge(g14, s14);
		PuM1.addEdge(s14, g15);
		PuM1.addEdge(g15, g16);
		PuM1.addEdge(g16, e12);

		PublicModel pum_acquirer = new PublicModel(PuM1, "Pum_Acquirer");
		IOUtils.toFile("AcquirerPubModel.dot", PuM1.toDOT());
		IOUtils.toFile("PuMAcuirerRpst.dot", pum_acquirer.toDOT());
		this.pum_acquirer = pum_acquirer;
		// IOUtils.toFile("AcquirerPubModelprojection.dot",pum_acquirer.projection(travelAgency,
		// "partner").toDOT());

		// Test for Complement function
		/*
		 * for (IRPSTNode<Edge<IPublicNode>, IPublicNode> node :
		 * pum_acquirer.getRPSTNodes()) { //System.out.print(node.getName() +
		 * ": fragment = " + node.getFragment()); if(node.getName().equals("P1") ||
		 * node.getName().equals("P4")) { System.out.println(node.getFragment());
		 * 
		 * System.out.println(pum_acquirer.Complement(node.getFragment(),
		 * acquirer).getGraph()); } }
		 */
	}

	private void buildPrivateAcquirerModel() {

		/************ Private Model of the Acquirer ******************************/
		MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode> PrM1 = new MultiDirectedGraph<Edge<IPrivateNode>, IPrivateNode>();

		this.pr11 = new Receive(travelAgency, m2, "receive_TA_check");
		this.ps12 = new Send(airline, m3, "send_A_failure");
		this.ps11 = new Send(travelAgency, m5, "send_TA_failure");
		this.ps13 = new Send(airline, m6, "send_A_payment");
		this.ps14 = new Send(travelAgency, m9, "send_TA_approval");
		this.pg11 = new XorGateway("g11");
		this.pg13 = new XorGateway("g13");
		this.pg12 = new XorGateway("g12");
		this.pg14 = new AndGateway("g14");
		this.pg15 = new AndGateway("g15");
		this.pg16 = new XorGateway("g16");
		this.pe11 = new Event("start");
		this.pe12 = new Event("end");
		this.pa11 = new PrivateActivity("priv_activity1");
		this.pa12 = new PrivateActivity("priv_activity2");
		this.pa13 = new PrivateActivity("priv_activity3");

		PrM1.addEdge(pe11, pr11);
		PrM1.addEdge(pr11, pa11);
		PrM1.addEdge(pa11, pg11);
		PrM1.addEdge(pg11, pa12);
		PrM1.addEdge(pa12, pg12);
		PrM1.addEdge(pg11, pa13);
		PrM1.addEdge(pa13, pg14);
		PrM1.addEdge(pg12, ps11);
		PrM1.addEdge(ps11, pg13);
		PrM1.addEdge(pg12, ps12);
		PrM1.addEdge(ps12, pg13);
		PrM1.addEdge(pg13, pg16);
		PrM1.addEdge(pg14, ps13);
		PrM1.addEdge(ps13, pg15);
		PrM1.addEdge(pg14, ps14);
		PrM1.addEdge(ps14, pg15);
		PrM1.addEdge(pg15, pg16);
		PrM1.addEdge(pg16, pe12);

		PrivateModel prm_acquirer = new PrivateModel(PrM1, "Prm_Acquirer");
		this.prm_acquirer = prm_acquirer;
		IOUtils.toFile("AcquirerPrModel.dot", PrM1.toDOT());
		IOUtils.toFile("AcuirerPrRpst.dot", prm_acquirer.toDOT());
		IOUtils.toFile("AcquirerPrModelInterface.dot", prm_acquirer.BehavioralInterface1().getdigraph().toDOT());
		IOUtils.toFile("AcquirerPrModelProjection.dot", prm_acquirer.projectionRole(travelAgency).getdigraph().toDOT());
	}

	private void buildPublicTravelAgencyModel() {
		// public model for travelAgency
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> TAGraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();

		Event TAstart = new Event("start");
		Event TAend = new Event("end");
		Receive ta1 = new Receive(traveler, this.m1, "book_trip");
		Send ta2 = new Send(acquirer, this.m2, "check & cash");
		this.ta3 = new Receive(acquirer, this.m3, "TA notification failure");
		this.ta4 = new Send(traveler, this.m4, "CC not approved");
		Receive ta7 = new Receive(airline, this.m7, "e-ticket");
		Receive ta8 = new Receive(airline, this.m8, "post ticket");
		Receive ta9 = new Receive(acquirer, this.m9, "approval");

		Gateway tag1 = new XorGateway("TA_g1");
		Gateway tag8 = new XorGateway("TA_g8");
		Gateway tag2 = new XorGateway("TA_g2");
		Gateway tag3 = new XorGateway("TA_g3");
		Gateway tag4 = new AndGateway("TA_g4");
		Gateway tag7 = new AndGateway("TA_g7");
		Gateway tag5 = new XorGateway("TA_g5");
		Gateway tag6 = new XorGateway("TA_g6");

		TAGraph.addEdge(TAstart, ta1);
		TAGraph.addEdge(ta1, ta2);
		TAGraph.addEdge(ta2, tag1);

		TAGraph.addEdge(tag1, tag2);
		TAGraph.addEdge(tag2, tag3);
		TAGraph.addEdge(tag2, ta3);
		TAGraph.addEdge(ta3, ta4);
		TAGraph.addEdge(ta4, tag3);
		TAGraph.addEdge(tag3, tag8);

		TAGraph.addEdge(tag1, tag4);
		TAGraph.addEdge(tag4, tag5);
		TAGraph.addEdge(tag5, ta7);
		TAGraph.addEdge(tag5, ta8);
		TAGraph.addEdge(ta7, tag6);
		TAGraph.addEdge(ta8, tag6);
		TAGraph.addEdge(tag6, tag7);

		TAGraph.addEdge(tag4, ta9);
		TAGraph.addEdge(ta9, tag7);

		TAGraph.addEdge(tag7, tag8);

		TAGraph.addEdge(tag8, TAend);

		pum_travelAgency = new PublicModel(TAGraph, "Pum_travelAgency");

		IOUtils.toFile("TravelAgencyModel.dot", pum_travelAgency.getdigraph().toDOT());
	}

	private void buildPublicAirlineModel() {
		// public model for airline
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> AGraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();

		Message m15 = new Message("ticket_purchase_canceled");

		Event Astart = new Event("start");
		Event Aend = new Event("end");
		Receive a5 = new Receive(airline, this.m5, "A_failure");
		Send a6 = new Send(acquirer, this.m6, "payment ok");
		Send a7 = new Send(travelAgency, this.m7, "e-ticket");
		Send a8 = new Send(travelAgency, this.m8, "post ticket");
		// Send a15 = new Send(traveler, m15, "ticket purchase canceled");

		Gateway ag1 = new XorGateway("A_g1");
		Gateway ag6 = new XorGateway("A_g6");
		Gateway ag2 = new XorGateway("A_g2");
		Gateway ag3 = new XorGateway("A_g3");
		Gateway ag4 = new XorGateway("A_g4");
		Gateway ag5 = new XorGateway("A_g5");

		AGraph.addEdge(Astart, ag1);
		AGraph.addEdge(ag1, ag2);
		AGraph.addEdge(ag2, a5);
		AGraph.addEdge(a5, ag3);
		AGraph.addEdge(ag2, ag3);
		AGraph.addEdge(ag3, ag6);
		AGraph.addEdge(ag1, a6);
		AGraph.addEdge(a6, ag4);
		AGraph.addEdge(ag4, a7);
		AGraph.addEdge(a7, ag5);
		AGraph.addEdge(ag4, a8);
		AGraph.addEdge(a8, ag5);
		AGraph.addEdge(ag5, ag6);
		AGraph.addEdge(ag6, Aend);

		pum_airline = new PublicModel(AGraph, "Pum_airline");

		IOUtils.toFile("AirlineModel.dot", pum_airline.getdigraph().toDOT());
	}

	private void buildPublicTravelerModel() {
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> TGraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();

		Event Tstart = new Event("start");
		Event Tend = new Event("end");
		Send a1 = new Send(travelAgency, this.m1, "book trip");
		Receive a4 = new Receive(travelAgency, this.m4, "credit card not approved");
		Gateway tg1 = new XorGateway("T_g1");
		Gateway tg2 = new XorGateway("T_g2");
		Gateway tg3 = new XorGateway("T_g3");
		Gateway tg4 = new XorGateway("T_g4");
		TGraph.addEdge(Tstart, a1);
		TGraph.addEdge(a1, tg1);
		TGraph.addEdge(tg1, tg2);
		TGraph.addEdge(tg2, a4);
		TGraph.addEdge(a4, tg3);
		TGraph.addEdge(tg2, tg3);
		TGraph.addEdge(tg3, tg4);
		TGraph.addEdge(tg1, tg4);
		TGraph.addEdge(tg4, Tend);

		pum_traveler = new PublicModel(TGraph, "Pum_traveler");

		IOUtils.toFile("TravelerModel.dot", pum_traveler.getdigraph().toDOT());

	}

	private void buildChoreography() {
		/******************** Building the choreography ***************************/

		List<Role> roles = new LinkedList<Role>();
		roles.add(traveler);
		roles.add(acquirer);
		roles.add(travelAgency);
		roles.add(airline);
		// Collaboration collaboration = new Collaboration(BookTrip, "booktrip",roles);
		// collaboration.AddPublicModel(pum_acquirer);
		List<PrivateModel> privatemodels = new LinkedList<PrivateModel>();

		// Choreography BookTripOperation = new
		// Choreography("booktripoperation",collaboration,privatemodels);
	}

	private void buildChoreography2() {
		List<Role> roles = new LinkedList<Role>(Arrays.asList(traveler, acquirer, travelAgency, airline));
		this.collab = new Collaboration("booktrip");
		this.collab.addPublicModel(acquirer, (PublicModel) pum_acquirer);
		this.collab.addPublicModel(travelAgency, (PublicModel) pum_travelAgency);
		this.collab.addPublicModel(airline, (PublicModel) pum_airline);
		this.collab.addPublicModel(traveler, (PublicModel) pum_traveler);
		this.collab.roles.add(acquirer);
		this.collab.roles.add(airline);
		this.collab.roles.add(travelAgency);
		this.collab.roles.add(traveler);
		List<IPrivateModel> privateModels = new LinkedList<IPrivateModel>();
		this.bookTripChoreography = new Choreography("booktripChoreo", collab, privateModels,
				(ChoreographyModel) BookTrip);

	}

	@Before
	public void setUp() {
		this.declareCommon();
		this.buildChoreographyModel();
		this.buildPublicAcquirerModel();
		this.buildPrivateAcquirerModel();
		this.buildPublicTravelAgencyModel();
		this.buildPublicAirlineModel();
		this.buildPublicTravelerModel();
		this.buildChoreography2();
	}

	@Test
	public <E extends Edge<N>, N extends INode> void assertIntersectTest() {

		Set<IChoreographyNode> nodes = new HashSet<IChoreographyNode>();
		System.out.println(this.BookTrip.getsuccessorsOfNode(i3, nodes));
		Set<IChoreographyNode> nodes2 = new HashSet<IChoreographyNode>();
		System.out.println(this.BookTrip.getpredecessorsOfNode(g4, nodes2));
		System.out.println(nodes.retainAll(nodes2));
		System.out.println(nodes);
	}

	private <E extends Edge<N>, N extends INode> void assertNodeIdsRemoved(String msg, RpstModel<E, N> model1,
			RpstModel<E, N> model2, String[] uuids_to_remove) {
		Set<String> uuids1 = FragmentUtil.collectNodeIds(model1.getRoot());
		Set<String> uuids2 = FragmentUtil.collectNodeIds(model2.getRoot());
		uuids1.removeAll(Arrays.asList(uuids_to_remove));
		assertEquals(msg, uuids1, uuids2);
	}

	private <E extends Edge<N>, N extends INode> void assertNodeIdsAdded(String msg, RpstModel<E, N> model1,
			RpstModel<E, N> model2, String[] uuids_to_add) {
		Set<String> uuids1 = FragmentUtil.collectNodeIds(model1.getRoot());
		Set<String> uuids2 = FragmentUtil.collectNodeIds(model2.getRoot());
		uuids1.addAll(Arrays.asList(uuids_to_add));
		assertEquals(msg, uuids1, uuids2);
	}

	private <E extends Edge<N>, N extends INode> void assertNodeIdsAddedAndRemoved(String msg, RpstModel<E, N> model1,
			RpstModel<E, N> model2, String[] uuids_to_add, String[] uuids_to_remove) {
		Set<String> uuids1 = FragmentUtil.collectNodeIds(model1.getRoot());
		Set<String> uuids2 = FragmentUtil.collectNodeIds(model2.getRoot());
		uuids1.addAll(Arrays.asList(uuids_to_add));
		uuids1.removeAll(Arrays.asList(uuids_to_remove));
		assertEquals(msg, uuids1, uuids2);
	}

	private <E extends Edge<N>, N extends INode> void compareModels(RpstModel<E, N> model1, RpstModel<E, N> model2) {
		Set<String> uuids1 = FragmentUtil.collectNodeIds(model1.getRoot());
		Set<String> uuids2 = FragmentUtil.collectNodeIds(model2.getRoot());
		HashMap<String, N> uuids2N1 = FragmentUtil.collectNodeIdMap(model1.getRoot());
		HashMap<String, N> uuids2N2 = FragmentUtil.collectNodeIdMap(model2.getRoot());

		// added? model1 -> model2
		for (String s : uuids1) {
			if (!uuids2.contains(s)) {
				System.out.println("removed (model1 -> model2): " + s + " N:" + uuids2N1.get(s));
			}
		}

		// removed? model1 -> model2
		for (String s : uuids2) {
			if (!uuids1.contains(s)) {
				System.out.println("added (model1 -> model2): " + s + " N:" + uuids2N2.get(s));
			}
		}
	}

	private <E extends Edge<N>, N extends INode> void assertNodeExists(String msg, RpstModel<E, N> model, N node) {
		Set<String> uuids = FragmentUtil.collectNodeIds(model.getRoot());
		assertEquals(msg, true, uuids.contains(node.getId()));
	}

	@Test
	public void deleteTrivialTest() {
		System.out.println("----ChoreographyTest::deleteTrivialTest()");
		IOUtils.toFile("BookTripRemoveGraphTrivial.dot", BookTrip.getdigraph().toDOT());
		IOUtils.toFile("BookTripRemoveRpstTrivial.dot", BookTrip.toDOT());

		// Trivial case: delete choreography node "I2" which is the single edge "I2" ->
		// "g1" in the Rpst (trivial fragment)
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete = BookTrip.getFragmentWithSource(this.i2);
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete2 = BookTrip.getFragmentWithTarget(this.i2);
		Set<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>> SetTrivialRpstNodes = new HashSet<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>>();
		System.out.println("sequence to delete:" + node2delete.getName() + " ----" + node2delete2.getFragment());
		SetTrivialRpstNodes.add(node2delete);
		SetTrivialRpstNodes.add(node2delete2);
		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter = BookTrip.delete(SetTrivialRpstNodes);

		IOUtils.toFile("BookTripRemoveGraphTrivial_after.dot", BookTripAfter.getdigraph().toDOT());
		IOUtils.toFile("BookTripRemoveRpstTrivial_after.dot", BookTripAfter.toDOT());

		// assert on nodes
		this.assertNodeIdsRemoved("After deleting I2: should have 1 node less", BookTrip, BookTripAfter,
				new String[] { i2.getId() });
		// Not always true if there is graph reduction ++ also it is not possible to
		// delete a single edge
		this.assertNodeIdsRemoved("After deleting I2 (directly)", BookTrip, BookTrip.delete(i2),
				new String[] { i2.getId() });
	}

	@Test
	public void deleteSingleEdgeTest() {
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete = BookTrip.getFragmentWithSource(this.i2);
		Set<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>> nodeSet = new HashSet<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>>();
		nodeSet.add(node2delete);
		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter = BookTrip.delete(nodeSet);
		// TODO: assert on exception
		this.assertNodeIdsRemoved("After deleting a single edge with I2 as source: node deletion should fail", BookTrip,
				BookTripAfter, new String[] {});
	}

	@Test
	public void deleteSingleNodeWithGraphReductionTest() {
		String graphFilename = "BookTripRemoveGraphSingleNodeGraphReduction";
		String rpstFilename = "BookTripRemoveRpstSingleNodeGraphReduction";

		System.out.println("----ChoreographyTest::deleteSingleNodeWithGraphReductionTest()");
		IOUtils.toFile(graphFilename + ".dot", BookTrip.getdigraph().toDOT());
		IOUtils.toFile(rpstFilename + ".dot", BookTrip.toDOT());

		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete = BookTrip.getFragmentWithSource(this.i9);
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete2 = BookTrip.getFragmentWithTarget(this.i9);
		Set<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>> nodeSet = new HashSet<IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode>>();
		nodeSet.add(node2delete);
		nodeSet.add(node2delete2);
		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter = BookTrip.delete(nodeSet);

		IOUtils.toFile(graphFilename + "_after.dot", BookTripAfter.getdigraph().toDOT());
		IOUtils.toFile(rpstFilename + "_after.dot", BookTripAfter.toDOT());
		this.assertNodeIdsRemoved("After deleting activity i9 -> graph reduction", BookTrip, BookTripAfter,
				new String[] { g4.getId(), i9.getId(), g7.getId() });
	}

	@Test
	public void deleteXORFragmentTest() {
		String graphFilename = "BookTripRemoveGraphXORFragment";
		String rpstFilename = "BookTripRemoveRpstXORFragment";

		System.out.println("----ChoreographyTest::deleteXORFragmentTest()");
		IOUtils.toFile(graphFilename + ".dot", BookTrip.getdigraph().toDOT());
		IOUtils.toFile(rpstFilename + ".dot", BookTrip.toDOT());

		// Boundary case: delete choreography node "g2" which is formed by the fragment
		// that holds both g2->I3 and g2->I5 (and the rest: I3->I4, I4->g3, I5->g3)
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete = BookTrip
				.getFragmentWithSourceOrTarget(this.g2);
		System.out.println("node to delete:" + node2delete.getName() + " fragment: " + node2delete.getFragment());

		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter = BookTrip.delete(node2delete);

		IOUtils.toFile(graphFilename + "_after.dot", BookTripAfter.getdigraph().toDOT());
		IOUtils.toFile(rpstFilename + "_after.dot", BookTripAfter.toDOT());

		// assert on nodes
		this.assertNodeIdsRemoved(
				"After deleting fragment holding g2: should have g2-->g3 path removed and replaced with empty path",
				BookTrip, BookTripAfter, new String[] { g2.getId(), i3.getId(), i4.getId(), i5.getId(), g3.getId() });

		// assert on the newly added edge g1->g8
		assertTrue(BookTripAfter.getFragmentTrivialEdge(this.g1, this.g8) != null);
	}

	@Test
	public void deleteANDFragmentTest() {
		String graphFilename = "BookTripRemoveGraphANDFragment";
		String rpstFilename = "BookTripRemoveRpstANDFragment";

		System.out.println("----ChoreographyTest::deleteANDFragmentTest()");
		IOUtils.toFile(graphFilename + ".dot", BookTrip.getdigraph().toDOT());
		IOUtils.toFile(rpstFilename + ".dot", BookTrip.toDOT());

		// Boundary case: delete choreography node "I9" which is within the fragment
		// that holds the AND-split g4 and AND-join g7;
		// results in graph reduction: g4 and g7 removed and g1 directly links to I6, g6
		// directly to g8. I9 is also removed.
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete = BookTrip
				.getFragmentWithSourceOrTarget(this.i9);
		System.out.println("node to delete:" + node2delete.getName() + " fragment: " + node2delete.getFragment());
		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter = BookTrip.delete(node2delete);

		IOUtils.toFile(graphFilename + "_after.dot", BookTripAfter.getdigraph().toDOT());
		IOUtils.toFile(rpstFilename + "_after.dot", BookTripAfter.toDOT());

		// assert nodes after graph reduction
		this.assertNodeIdsRemoved("After deleting fragment that holds i9 -> graph reduction", BookTrip, BookTripAfter,
				new String[] { g4.getId(), i9.getId(), g7.getId() });
	}

	@Test
	public void insertTest() {
		// new fragment with single interaction -> we add it after the existing i2
		MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode> newFragmentGraph = new MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>();
		IChoreographyNode iNew = new Interaction("INew", travelAgency, acquirer, this.m2);
		Event e1 = new Event("start");
		Event e2 = new Event("end");
		newFragmentGraph.addEdge(e1, iNew);
		newFragmentGraph.addEdge(iNew, e2);
		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> B2 = new ChoreographyModel(newFragmentGraph);

		IOUtils.toFile("InsertGraph.dot", BookTrip.getdigraph().toDOT());
		IOUtils.toFile("InsertRpst.dot", BookTrip.toDOT());

		RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter = BookTrip.insert(B2.getRoot(),
				BookTrip.getdigraph().getEdge(this.i2, this.g1));

		IOUtils.toFile("InsertGraph_after.dot", BookTripAfter.getdigraph().toDOT());
		IOUtils.toFile("InsertRpst_after.dot", BookTripAfter.toDOT());

		// assert that the new node is in the graph
		this.assertNodeIdsAdded("iNew should occur after insertion", BookTrip, BookTripAfter,
				new String[] { iNew.getId() });

		// we make sure that the insert operation is non-destructive, and that
		// it returns a new instance with the modified graph
		this.assertNodeIdsRemoved("iNew should not exist in the original instance", BookTripAfter, BookTrip,
				new String[] { iNew.getId() });
	}

	@Test
	public void replaceTest() {
		// TODO: perform replace tests on Acquirer
		// Replacing the XOR(Send(travelAgency,m3,
		// "send_TA_failure"),Send(travelAgency,m5, "send_TA_failure"))
		// by Sequence(Send(travelAgency,m5, "send_TA_failure"),Send(travelAgency,m5,
		// "send_TA_failure"))

		// Creating a new fragment
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> newFragmentgraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
		Event e1 = new Event("start");
		Event e2 = new Event("end");
		IPublicNode s11 = new Send(airline, m5, "send_A_failure");
		IPublicNode s12 = new Send(travelAgency, m3, "send_TA_failure");
		newFragmentgraph.addEdge(e1, s11);
		newFragmentgraph.addEdge(s11, s12);
		newFragmentgraph.addEdge(s12, e2);
		PublicModel fragment = new PublicModel(newFragmentgraph, "fragment");

		IOUtils.toFile("ReplaceGraph.dot", pum_acquirer.getdigraph().toDOT());
		IOUtils.toFile("ReplaceRpst.dot", pum_acquirer.toDOT());

		// Looking for the old fragment to be replaced and replace it
		IRPSTNode<Edge<IPublicNode>, IPublicNode> node2replace = pum_acquirer.getFragmentWithSourceOrTarget(this.g12);
		pum_acquirer = pum_acquirer.replace(node2replace, fragment.getRoot());

		IOUtils.toFile("NewFragment1.dot", fragment.getdigraph().toDOT());
		IOUtils.toFile("NewFragment2.dot", fragment.toDOT());
		IOUtils.toFile("ReplaceGraph_after.dot", pum_acquirer.getdigraph().toDOT());
		IOUtils.toFile("ReplaceRpst_after.dot", pum_acquirer.toDOT());

		// assert that g12 and g13 are gone in the replaced graph
		assertEquals("g12 should be gone now", null, pum_acquirer.getFragmentWithSourceOrTarget(this.g12));
		assertEquals("g13 should be gone as well", null, pum_acquirer.getFragmentWithSourceOrTarget(this.g13));

		// assert that the new fragment contains s11, s12, as well as g11 and g16
		IRPSTNode<Edge<IPublicNode>, IPublicNode> newFragment = pum_acquirer.getFragmentWithSourceOrTarget(s11);
		Set<String> expectedIds = new HashSet<String>(
				Arrays.asList(new String[] { s11.getId(), s12.getId(), this.g11.getId(), this.g16.getId() }));
		assertEquals("The new fragment should contain s11,s12,g11 and g16", expectedIds,
				FragmentUtil.collectNodeIds(newFragment));
	}

	@Test
	public void replaceIdenticalTest() {
		// creating an identical fragment
		MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> identicalFragmentGraph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>();
		Event e1 = new Event("start");
		Event e2 = new Event("end");

		identicalFragmentGraph.addEdge(e1, this.g12);
		identicalFragmentGraph.addEdge(this.g12, this.s11);
		identicalFragmentGraph.addEdge(this.g12, this.s12);
		identicalFragmentGraph.addEdge(this.s11, this.g13);
		identicalFragmentGraph.addEdge(this.s12, this.g13);
		identicalFragmentGraph.addEdge(this.g13, e2);

		PublicModel fragment = new PublicModel(identicalFragmentGraph, "fragment");

		IOUtils.toFile("ReplaceIdenticalGraph.dot", pum_acquirer.getdigraph().toDOT());
		IOUtils.toFile("ReplaceIdenticalRpst.dot", pum_acquirer.getdigraph().toDOT());

		IRPSTNode<Edge<IPublicNode>, IPublicNode> node2replace = pum_acquirer.getFragmentWithSourceOrTarget(this.g12);
		RpstModel<Edge<IPublicNode>, IPublicNode> pum_acquirer_after = pum_acquirer.replace(node2replace,
				fragment.getRoot());

		IOUtils.toFile("ReplaceIdenticalGraph_after.dot", pum_acquirer_after.getdigraph().toDOT());
		IOUtils.toFile("ReplaceIdenticalRpst_after.dot", pum_acquirer_after.getdigraph().toDOT());

		// assertions: g12 and g13 should still exist
		assertTrue(pum_acquirer_after.getFragmentWithSourceOrTarget(this.g12) != null);
		assertTrue(pum_acquirer_after.getFragmentWithSourceOrTarget(this.g13) != null);

		// assert that the same node ids are there
		this.assertNodeIdsRemoved("The identical replace should result in an intact graph:", pum_acquirer,
				pum_acquirer_after, new String[] {});
	}

	@Test
	public void projectionRoleChoreographyModelTest() {
		// test choreography model
		Map<Role, INode[]> roleToExpectedNodes = new HashMap<Role, INode[]>();
		roleToExpectedNodes.put(traveler, new INode[] { this.i1, this.i4 });
		roleToExpectedNodes.put(travelAgency,
				new INode[] { this.i1, this.i2, this.i3, this.i4, this.i7, this.i8, this.i9, this.i10, this.i11 });
		roleToExpectedNodes.put(acquirer,
				new INode[] { this.i2, this.i3, this.i5, this.i6, this.i9, this.i10, this.i11 });
		roleToExpectedNodes.put(airline, new INode[] { this.i5, this.i6, this.i7, this.i8 });
		INode[] commonNodes = new INode[] { this.e1, this.e2, this.g1, this.g2, this.g3, this.g4, this.g5, this.g6,
				this.g7, this.g8 };

		this.assertProjectionRoles(BookTrip, roleToExpectedNodes, commonNodes);
	}

	@Test
	public void projectionRolePublicModelTest() {
		Map<Role, INode[]> roleToExpectedNodes = new HashMap<Role, INode[]>();
		roleToExpectedNodes.put(travelAgency, new INode[] { this.r11, this.s12, this.s14 });
		roleToExpectedNodes.put(airline, new INode[] { this.s11, this.s13 });
		INode[] commonNodes = new INode[] { this.e11, this.e12, this.g11, this.g12, this.g13, this.g14, this.g15,
				this.g16 };

		this.assertProjectionRoles(pum_acquirer, roleToExpectedNodes, commonNodes);
	}

	@Test
	public void projectionRolePrivateModelTest() {
		Map<Role, INode[]> roleToExpectedNodes = new HashMap<Role, INode[]>();
		roleToExpectedNodes.put(travelAgency, new INode[] { this.pr11, this.ps11, this.ps14 });
		roleToExpectedNodes.put(airline, new INode[] { this.ps12, this.ps13 });
		INode[] commonNodes = new INode[] { this.pe11, this.pe12, this.pg11, this.pg12, this.pg13, this.pg14, this.pg15,
				this.pg16 };

		this.assertProjectionRoles(prm_acquirer, roleToExpectedNodes, commonNodes);
	}

	private <E extends Edge<N>, N extends INode> void assertProjectionRoles(RpstModel<E, N> model,
			Map<Role, INode[]> expectedRoleToNodes, INode[] commonNodes) {
		for (Entry<Role, INode[]> entry : expectedRoleToNodes.entrySet()) {
			Role role = entry.getKey();
			INode[] nodesToExpect = entry.getValue();

			// we only check projection, no reduction (these are done elsewhere)
			RpstModel<E, N> projectedModel = (RpstModel<E, N>) model.projectionRole(role, false);
			Set<String> commonNodeIds = this.getIdSetFromNodes(commonNodes);
			Set<String> expectedNodeIds = this.getIdSetFromNodes(nodesToExpect);
			expectedNodeIds.addAll(commonNodeIds);

			assertEquals("project role on " + role.name, expectedNodeIds,
					FragmentUtil.collectNodeIds(projectedModel.getRoot()));
		}
	}

	private Set<String> getIdSetFromNodes(INode[] nodes) {
		Set<String> nodeIds = new HashSet<String>();
		for (INode n : nodes) {
			nodeIds.add(n.getId());
		}
		return nodeIds;
	}

	private <E extends Edge<N>, N extends INode> void assertNodes(String msg, N[] expected, N[] actual) {
		Set<String> expectedNodes = this.getIdSetFromNodes(expected);
		Set<String> actualNodes = this.getIdSetFromNodes(actual);
		assertEquals(msg, expectedNodes, actualNodes);
	}

	// ====
	// ==== PostSet Tests ====//
	// ====

	@Test
	public void postSetChoreographyTest() {

		// postSet on choreography model
		// i2: i3,i4,i5,i6,i7,i8,i9
		Set<IChoreographyNode> postset = BookTrip.getPostSet(BookTrip.getFragmentWithSource(this.i2));
		this.assertNodes("the post set of choreography model BookTrip should be",
				new INode[] { this.i3, this.i4, this.i5, this.i6, this.i7, this.i8, this.i9 },
				postset.toArray(new INode[0]));

		// transitive postset on choreography model
		// i2, role=travelAgency: i7,i8,i9,i3
		Set<IChoreographyNode> tPostSet = BookTrip.getTransitivePostSet(BookTrip.getFragmentWithSource(this.i2),
				travelAgency);
		this.assertNodes("the transitive post set of choreography model BookTrip:",
				new INode[] { this.i7, this.i8, this.i9, this.i3 }, tPostSet.toArray(new INode[0]));

		// get the smallest fragment of the transitive post set
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> smallestF = BookTrip
				.getTransitivePostSetF(BookTrip.getFragmentWithSource(this.i2), travelAgency);
		assertEquals("Smallest fragment of transitive postset (choreography model):", smallestF,
				BookTrip.getFragmentBoundedBy(this.g1, this.g8));

		Set<IChoreographyNode> postset1 = BookTrip.getPostSet(BookTrip.getFragmentBoundedBy(this.g2, this.g3));
		assertEquals("postset of Fragment(g2,g3) is an empty set", postset1.size(), 0);

		Set<IChoreographyNode> tPostSet1 = BookTrip
				.getTransitivePostSet(BookTrip.getFragmentBoundedBy(this.g2, this.g3), travelAgency);
		assertEquals("transitive postset of Fragment(g2,g3) is an empty set", tPostSet1.size(), 0);

		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> smallestF1 = BookTrip
				.getTransitivePostSetF(BookTrip.getFragmentBoundedBy(this.g2, this.g3), travelAgency);
		assertEquals("Smallest fragment of transitive postset Fragment(g2,g3) is null", smallestF1, null);
	}

	@Test
	public void postSetPublicTest() {
		// postSet on public model
		// receive_TA_check(r11): send_A_failure(s11), send_TA_failre(s12),
		// send_A_paymen(s13), send_TA_approval(s14)
		Set<IPublicNode> postset = pum_acquirer.getPostSet(pum_acquirer.getFragmentWithSource(this.r11));
		this.assertNodes("the postset of public model pum_acquirer should be",
				new INode[] { this.s11, this.s12, this.s13, this.s14 }, postset.toArray(new INode[0]));

		// transitive postset on public model
		// receive_TA_check(r11), role=travelAgency: send_TA_failure(s12),
		// send_TA_approval(s14)
		Set<IPublicNode> tPostSet = pum_acquirer.getTransitivePostSet(pum_acquirer.getFragmentWithSource(this.r11),
				travelAgency);
		this.assertNodes("the transitive postset of public model pum_acquirer", new INode[] { this.s12, this.s14 },
				tPostSet.toArray(new INode[0]));

		// get the smallest fragment of the transitive post set
		IRPSTNode<Edge<IPublicNode>, IPublicNode> smallestF = pum_acquirer
				.getTransitivePostSetF(pum_acquirer.getFragmentWithSource(this.r11), travelAgency);
		assertEquals("Smallest fragment of transitive postset (public model):", smallestF,
				pum_acquirer.getFragmentBoundedBy(this.g11, this.g16));
	}

	// @Test
	public void postSetPrivateTest() {
		// postSet on private model
		// priv_activity(pa13): send_TA_approval(ps13), send_A_payment(ps14)
		Set<IPrivateNode> postset = prm_acquirer.getPostSet(prm_acquirer.getFragmentWithSource(this.pa13));
		this.assertNodes("the postset of private model prm_acquirer should be", new INode[] { this.ps13, this.ps14 },
				postset.toArray(new INode[0]));

		// transitive postset on private model
		// priv_activity3(pa13), role=travelAgency: send_TA_approval(ps14)
		Set<IPrivateNode> tPostSet = prm_acquirer.getTransitivePostSet(prm_acquirer.getFragmentWithSource(this.pa13),
				travelAgency);
		this.assertNodes("the transitive postset of prm_acquirer should be", new INode[] { this.ps14 },
				tPostSet.toArray(new INode[0]));

		// get the smallest fragment of the transitive post set
		IRPSTNode<Edge<IPrivateNode>, IPrivateNode> smallestF = prm_acquirer
				.getTransitivePostSetF(prm_acquirer.getFragmentWithSource(this.pa13), travelAgency);
		assertEquals("Smallest fragment of transitive postset (private model):", smallestF,
				prm_acquirer.getFragmentWithSourceOrTarget(this.ps14));
	}

	// ====
	// ==== PreSet Tests ====//
	// ====

	// @Test
	public void preSetChoreographyTest() {
		// preSet on choreography model
		// fragment g2-->g3: I1,I10,I11,I2
		Set<IChoreographyNode> preset = BookTrip.getPreSet(BookTrip.getFragmentBoundedBy(this.g2, this.g3));
		this.assertNodes("the preset of BookTrip should be", new INode[] { this.i1, this.i10, this.i11, this.i2 },
				preset.toArray(new INode[0]));

		// transitive preSet on choreography model
		// fragment g2-->g3, role=travelAgency: I2
		Set<IChoreographyNode> tPreSet = BookTrip.getTransitivePreSet(BookTrip.getFragmentBoundedBy(this.g2, this.g3),
				travelAgency);
		this.assertNodes("the transitive preset of BookTrip should be", new INode[] { this.i2 },
				tPreSet.toArray(new INode[0]));

		// get the smallest fragment of the transitive preset
		IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> smallestF = BookTrip
				.getTransitivePreSetF(BookTrip.getFragmentBoundedBy(this.g2, this.g3), travelAgency);
		assertEquals("Smallest fragment of transitive preset (choreography model):", smallestF,
				BookTrip.getFragmentWithSourceOrTarget(this.i2));
	}

	// @Test
	public void preSetPublicTest() {
		// preset on public model
		// fragment g12-->g13: receive_TA_check(r11)
		Set<IPublicNode> preset = pum_acquirer.getPreSet(pum_acquirer.getFragmentBoundedBy(this.g12, this.g13));
		this.assertNodes("the preset of pum_acquirer should be", new INode[] { this.r11 },
				preset.toArray(new INode[0]));

		// transitive preset on public model
		// fragment g14-->g15, role=travelAgency: receive_TA_check(r11)
		Set<IPublicNode> tPreSet = pum_acquirer
				.getTransitivePreSet(pum_acquirer.getFragmentBoundedBy(this.g14, this.g15), travelAgency);
		this.assertNodes("the transitive preset of pum_acquirer should be", new INode[] { this.r11 },
				tPreSet.toArray(new INode[0]));

		// get the smallest fragment of the transitive preset
		IRPSTNode<Edge<IPublicNode>, IPublicNode> smallestF = pum_acquirer
				.getTransitivePreSetF(pum_acquirer.getFragmentBoundedBy(this.g14, this.g15), travelAgency);
		assertEquals("Smallest fragment of transitive preset (public model):", smallestF,
				pum_acquirer.getFragmentWithSourceOrTarget(this.r11));
	}

	// @Test
	public void Transitivepreset() {
		Set<IChoreographyNode> set = new HashSet<IChoreographyNode>();
		set.add(this.g2);
		assertEquals("transitive preset of ", set, BookTrip.getTransitivePreSetWithGateways(this.i3, acquirer));
		set = new HashSet<IChoreographyNode>();
		set.add(this.i5);
		set.add(this.i3);
		assertEquals("transitive preset of ", set, BookTrip.getTransitivePreSetWithGateways(this.g3, acquirer));
		set = new HashSet<IChoreographyNode>();
		set.add(this.i6);
		assertEquals("transitive preset of ", set, BookTrip.getTransitivePreSetWithGateways(this.g6, acquirer));
		set = new HashSet<IChoreographyNode>();
		set.add(this.g1);
		assertEquals("transitive preset of ", set, BookTrip.getTransitivePreSetWithGateways(this.g7, traveler));
	}

	// @Test
	public void preSetPrivateTest() {
		// preset on private model
		// priv_activity3(pa13): receive_TA_check(pr11), priv_activity1(pa11)
		Set<IPrivateNode> preset = prm_acquirer.getPreSet(prm_acquirer.getFragmentWithSourceOrTarget(this.pa13));
		this.assertNodes("the preset of private model prm_acquirer should be", new INode[] { this.pr11, this.pa11 },
				preset.toArray(new INode[0]));

		// transitive preset on private model
		// priv_activity3(pa13), role=travelAgency: receive_TA_check(pr11)
		Set<IPrivateNode> tPreSet = prm_acquirer
				.getTransitivePreSet(prm_acquirer.getFragmentWithSourceOrTarget(this.pa13), travelAgency);
		this.assertNodes("the transitive preset of private model prm_acquirer should be", new INode[] { this.pr11 },
				tPreSet.toArray(new INode[0]));

		// get the smallest fragment of the transitive preset
		IRPSTNode<Edge<IPrivateNode>, IPrivateNode> smallestF = prm_acquirer
				.getTransitivePreSetF(prm_acquirer.getFragmentWithSourceOrTarget(this.pa13), travelAgency);
		assertEquals("Smallest fragment of transitive preset (public model):", smallestF,
				prm_acquirer.getFragmentWithSourceOrTarget(this.pr11));
	}

	/*
	 * //@Test public void changePropagationReplaceReplaceTest() {
	 * System.out.println("--changePropagationReplaceReplaceTest()"); // build
	 * collaboration buildChoreography2(); // old fragment:
	 * IRPSTNode<Edge<IPublicNode>, IPublicNode> f1 =
	 * pum_acquirer.getFragmentBoundedBy(this.g12, this.g13);
	 * 
	 * // new fragment: start -> send_TA_failure -> end
	 * MultiDirectedGraph<Edge<IPublicNode>, IPublicNode> f2Graph = new
	 * MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>(); // RPST needs > 1
	 * activities to correctly build Event start = new Event("start"); Event end =
	 * new Event("end"); IPublicNode s12 = new Send(travelAgency, m3,
	 * "send_TA_failure"); // TODO: should we reuse the original this.s12 in case of
	 * Id comparison? The underlying equality check does this.
	 * f2Graph.addEdge(start, s12); f2Graph.addEdge(s12, end); PublicModel f2 = new
	 * PublicModel(f2Graph, "fragment");
	 * 
	 * // perform change propagation (ignore transitive) Replace<Edge<IPublicNode>,
	 * IPublicNode> replace = new Replace<Edge<IPublicNode>, IPublicNode>(f1, f2,
	 * pum_acquirer, bookTripChoreography, acquirer); Map<String, Stats>
	 * chgOpId2Stats = new HashMap<String, Stats>(); Map<Role,
	 * List<IChangeOperation>> changeOps = ChangePropagationUtil.propagate(replace,
	 * null, null, chgOpId2Stats);
	 * 
	 * // assert: there are 2 changes. One for each partner (excluding traveler,
	 * which is transitive) assertEquals("there should be 2 changes", 2,
	 * changeOps.size());
	 * 
	 * // assert change operations on travelAgency List<IChangeOperation>
	 * changeOps_travelAgency = changeOps.get(travelAgency);
	 * assertEquals("after change prop: single replace on travelAgency's fragment",
	 * 1, changeOps_travelAgency.size()); // assert change operations on airline
	 * List<IChangeOperation> changeOps_airline = changeOps.get(airline);
	 * assertEquals("after change prop: single delete operation on airline's fragment"
	 * , 1, changeOps_airline.size());
	 * 
	 * System.out.println("Propagation stats---"+chgOpId2Stats); }
	 * 
	 * 
	 * // @Test public void changePropagationDeleteDeleteTest() {
	 * System.out.println("--changePropagationDeleteDeleteTest()"); // build
	 * collaboration buildChoreography2(); // old fragment to be deleted:
	 * IRPSTNode<Edge<IPublicNode>, IPublicNode> f1 =
	 * pum_acquirer.getFragmentBoundedBy(this.g12, this.g13);
	 * 
	 * // perform change propagation (ignore transitive) Delete<Edge<IPublicNode>,
	 * IPublicNode> delete = new Delete<Edge<IPublicNode>, IPublicNode>(f1,
	 * pum_acquirer, bookTripChoreography, acquirer); Map<String, Stats>
	 * chgOpId2Stats = new HashMap<String, Stats>(); Map<Role,
	 * List<IChangeOperation>> changeOps =
	 * ChangePropagationUtil.propagate(delete,null, null, chgOpId2Stats);
	 * 
	 * // assert: there are 2 changes. One for each partner (excluding traveler,
	 * which is transitive) assertEquals("there should be 2 changes", 2,
	 * changeOps.size());
	 * 
	 * // assert change operations on travelAgency List<IChangeOperation>
	 * changeOps_travelAgency = changeOps.get(travelAgency);
	 * assertEquals("after change prop: single delete on travelAgency's fragment",
	 * 1, changeOps_travelAgency.size()); // assert change operations on airline
	 * List<IChangeOperation> changeOps_airline = changeOps.get(airline);
	 * assertEquals("after change prop: single delete operation on airline's fragment"
	 * , 1, changeOps_airline.size());
	 * 
	 * System.out.println("Propagation stats---"+chgOpId2Stats); }
	 * 
	 * //@Test public void changePropagationInsertInsertTest() {
	 * System.out.println("--changePropagationInsertInsertTest()");
	 * 
	 * // build collaboration buildChoreography2(); // new fragment: start ->
	 * send_TA_failure -> end MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>
	 * f2Graph = new MultiDirectedGraph<Edge<IPublicNode>, IPublicNode>(); // RPST
	 * needs > 1 activities to correctly build Event start = new Event("start");
	 * Event end = new Event("end"); IPublicNode s12 = new Send(travelAgency, m3,
	 * "send_TA_failure"); // TODO: should we reuse the original this.s12 in case of
	 * Id comparison? The underlying equality check does this.
	 * 
	 * f2Graph.addEdge(start, s12); f2Graph.addEdge(s12, end); PublicModel f2 = new
	 * PublicModel(f2Graph, "fragment");
	 * 
	 * // perform change propagation (ignore transitive) Insert<Edge<IPublicNode>,
	 * IPublicNode> insert = new Insert<Edge<IPublicNode>, IPublicNode>(f2,
	 * pum_acquirer,this.e11,this.r11 , bookTripChoreography, acquirer); Map<String,
	 * Stats> chgOpId2Stats = new HashMap<String, Stats>(); Map<Role,
	 * List<IChangeOperation>> changeOps = ChangePropagationUtil.propagate(insert,
	 * null, null, chgOpId2Stats);
	 * 
	 * // assert: there are 2 changes. One for each partner (excluding traveler,
	 * which is transitive) assertEquals("there should be 1 changes", 1,
	 * changeOps.size()); // assert change operations on travelAgency
	 * List<IChangeOperation> changeOps_travelAgency = changeOps.get(travelAgency);
	 * assertEquals("after change prop: single insert on travelAgency's fragment",
	 * 1, changeOps_travelAgency.size());
	 * 
	 * System.out.println("Propagation stats---"+chgOpId2Stats); }
	 * 
	 * // @Test public void SeveralChangeScenariosTest() throws IOException,
	 * WriteException{
	 * 
	 * System.out.println("--ChangePropagationStatisticsTest"); // build
	 * collaboration buildChoreography2(); //generate the set of possible change
	 * operations organized by type and role. Map<IRole, Map<ChgOpType,
	 * Set<IChangeOperation>>> role2map =
	 * ChangePropagationUtil.generateChangeOperationsForPublicModels(
	 * bookTripChoreography); ChangePropagationUtil.init(bookTripChoreography);
	 * Map<String, Stats> chgOpId2Stats = new HashMap<String, Stats>(); //Change
	 * Prpagation for each generated change request for(IRole role :
	 * role2map.keySet()){ Map<ChgOpType, Set<IChangeOperation>> role2map1 =
	 * role2map.get(role); for(ChgOpType type : role2map1.keySet())
	 * for(IChangeOperation op :role2map.get(role).get(type))
	 * ChangePropagationUtil.propagate(op,null, null, chgOpId2Stats); }
	 * 
	 * //Creating an excel file and storing all metrics WritableWorkbook workbook =
	 * Workbook.createWorkbook(new File("target/ChangePropagationStats.xls"));
	 * WritableSheet sheet = workbook.createSheet("propagationSheet",0);
	 * 
	 * Label ChgOpId = new Label(0,0,"ChgOpId"); Label type = new Label(1,0,"Type");
	 * Label Nb_Nodes_source = new Label(2,0,"Nb_Nodes_source"); Label
	 * nb_activ_source = new Label(3,0,"nb_activ_source"); Label nb_AndGtw_source =
	 * new Label(4,0,"nb_AndGtw_source"); Label nb_XorGtw_source = new
	 * Label(5,0,"nb_XorGtw_source"); //Metrics of the propagation result Label
	 * Nb_Nodes_target = new Label(7,0,"Nb_Nodes_target"); Label nb_activ_target =
	 * new Label(8,0,"nb_activ_target"); Label nb_AndGtw_target = new
	 * Label(9,0,"nb_AndGtw_target"); Label nb_XorGtw_target = new
	 * Label(10,0,"nb_XorGtw_target"); Label nb_affected_partners = new
	 * Label(11,0,"nb_affected_partners"); Label nb_Insert_genreated = new
	 * Label(12,0,"nb_Insert_genreated"); Label nb_Replace_generated = new
	 * Label(13,0,"nb_Replace_generated"); Label nb_Delete_generated = new
	 * Label(14,0,"nb_Delete_generated");
	 * 
	 * sheet.addCell(nb_XorGtw_target);sheet.addCell(nb_AndGtw_target);sheet.addCell
	 * (nb_activ_target);sheet.addCell(Nb_Nodes_target);
	 * sheet.addCell(nb_Delete_generated);sheet.addCell(nb_Replace_generated);sheet.
	 * addCell(nb_Insert_genreated);sheet.addCell(nb_affected_partners);
	 * sheet.addCell(nb_XorGtw_source);sheet.addCell(nb_AndGtw_source);sheet.addCell
	 * (nb_activ_source);sheet.addCell(Nb_Nodes_source);
	 * sheet.addCell(type);sheet.addCell(ChgOpId);
	 * 
	 * int i=1; for(String opId : chgOpId2Stats.keySet()){ Stats stats =
	 * chgOpId2Stats.get(opId); sheet.addCell(new Label(0, i, opId));
	 * sheet.addCell(new Label(1, i, stats.type.toString())); sheet.addCell(new
	 * Label(2, i, Integer.toString(stats.Nb_Nodes_source))); sheet.addCell(new
	 * Label(3, i, Integer.toString(stats.nb_activ_source))); sheet.addCell(new
	 * Label(4, i, Integer.toString(stats.nb_AndGtw_source))); sheet.addCell(new
	 * Label(5, i, Integer.toString(stats.nb_XorGtw_source))); sheet.addCell(new
	 * Label(7, i, Integer.toString(stats.Nb_Nodes_target))); sheet.addCell(new
	 * Label(8, i, Integer.toString(stats.nb_activ_target))); sheet.addCell(new
	 * Label(9, i, Integer.toString(stats.nb_AndGtw_target))); sheet.addCell(new
	 * Label(10, i, Integer.toString(stats.nb_XorGtw_target))); sheet.addCell(new
	 * Label(11, i, Integer.toString(stats.nb_affected_partners)));
	 * sheet.addCell(new Label(12, i, Integer.toString(stats.nb_Insert_generated)));
	 * sheet.addCell(new Label(13, i,
	 * Integer.toString(stats.nb_Replace_generated))); sheet.addCell(new Label(14,
	 * i, Integer.toString(stats.nb_Delete_generated))); i++; } //Write and close
	 * the workbook workbook.write(); workbook.close();
	 * 
	 * 
	 * //Impact Anlysis test //UNCOMMENT //System.out.println("---centrality");
	 * //System.out.println(ChangePropagationUtil.centrality);
	 * //System.out.println("---Propagation graph");
	 * //System.out.println(ChangePropagationUtil.PropagationGraphMetrics);
	 * for(Pair<IRole,IRole> pair :
	 * ChangePropagationUtil.PropagationGraphMetrics.keySet()){
	 * System.out.println("<"+pair.first+" , "+pair.second+"> -->"
	 * +ChangePropagationUtil.PropagationGraphMetrics.get(pair)); }
	 * 
	 * }
	 * 
	 * 
	 * @Test public void ChangeOperationsGenratorTest(){
	 * System.out.println("--changeOperationGeneratorTest"); // build collaboration
	 * buildChoreography2(); //generate the set of possible change operations
	 * organized by type and role. Map<IRole, Map<ChgOpType, Set<IChangeOperation>>>
	 * role2map = ChangePropagationUtil.generateChangeOperationsForPublicModels(
	 * bookTripChoreography); for(IRole role : role2map.keySet()){ Map<ChgOpType,
	 * Set<IChangeOperation>> role2map1 = role2map.get(role); for(ChgOpType type :
	 * role2map1.keySet()){ Set<IChangeOperation> chgOpSet =
	 * role2map.get(role).get(type); // System.out.println("Role = " + role +
	 * "   --ChgOpType="+type + "  --ChgOpSet=  " + chgOpSet); IPublicModel pum =
	 * collab.R2PuM.get(role); int expected = 0; if(type == ChgOpType.Insert)
	 * expected = pum.getRPSTNodes().size() *
	 * pum.getRPSTNodes(TCType.TRIVIAL).size(); if(type == ChgOpType.Delete)
	 * expected = pum.getRPSTNodes().size(); if(type == ChgOpType.Replace) expected
	 * = pum.getRPSTNodes().size() * pum.getRPSTNodes().size();
	 * assertEquals("number of chge operations of type " + type
	 * +" for the role"+role+"= ", expected, chgOpSet.size()); } } }
	 * 
	 * 
	 * @Test public void generateCloneModelFromFragmentTest(){
	 * System.out.println("--generateCloneModelFromFragmentTest...");
	 * RpstModel<Edge<IPublicNode>,IPublicNode> clonemodel =
	 * FragmentUtil.generateCloneModelFromFragment(pum_acquirer.getFragmentBoundedBy
	 * (g12, g13), pum_acquirer);
	 * System.out.println("original fragment = "+pum_acquirer.getFragmentBoundedBy(
	 * g12, g13)); System.out.println("cloned model = " +clonemodel);
	 * IOUtils.toFile("CloneModelFromFragment.dot",
	 * clonemodel.getdigraph().toDOT());
	 * IOUtils.toFile("CloneModelFromFragmentRPST.dot", clonemodel.toDOT()); //the
	 * number of edges of the generates model should have two more edges (bcoz we
	 * added start and end events) int expected =
	 * pum_acquirer.getFragmentBoundedBy(g12, g13).getFragment().size()+2;
	 * //assertEquals("after model generation, the number of edges  ",
	 * clonemodel.getRoot().getFragment().size(), expected); //exception: testing
	 * clone of the whole model --> should have the same number of edges clonemodel
	 * = FragmentUtil.generateCloneModelFromFragment(pum_acquirer.getRoot(),
	 * pum_acquirer); expected = pum_acquirer.getRoot().getFragment().size();
	 * //assertEquals("after model generation, the number of edges  ",
	 * clonemodel.getRoot().getFragment().size(), expected );
	 * 
	 * System.out.println("--getAllCloneFragmentModelsTest...");
	 * Set<RpstModel<Edge<IPublicNode>,IPublicNode>> clonemodels =
	 * FragmentUtil.getAllCloneFragmentModels(pum_acquirer); //
	 * assertEquals("number of generated models",
	 * clonemodels.size(),pum_acquirer.getRPSTNodes(TCType.BOND).size()); int i = 0;
	 * for(RpstModel<Edge<IPublicNode>,IPublicNode> m : clonemodels){
	 * IOUtils.toFile("clone/GenModel"+i+".dot", m.getdigraph().toDOT()); i++; } }
	 * 
	 * 
	 * @Test public void fragmentEqualityTest() { // true case
	 * IRPSTNode<Edge<IPublicNode>,IPublicNode> f1 =
	 * pum_acquirer.getFragmentBoundedBy(this.g12, this.g13);
	 * 
	 * MultiDirectedGraph<Edge<IPublicNode>,IPublicNode> f2Graph = new
	 * MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>(); //Event start = new
	 * Event("start"); //Event end = new Event("end"); Gateway g12New = new
	 * AndGateway("g12New"); Gateway g13New = new AndGateway("g13New");
	 * //f2Graph.addEdge(start, g12New); f2Graph.addEdge(g12New,s11);
	 * f2Graph.addEdge(g12New,s12); f2Graph.addEdge(s11,g13New);
	 * f2Graph.addEdge(s12,s13); f2Graph.addEdge(s13,g13New);
	 * //f2Graph.addEdge(g13New, end); PublicModel f2 = new PublicModel(f2Graph,
	 * "fragment");
	 * 
	 * assertEquals("fragment comparison should return equal", true,
	 * FragmentUtil.fragmentIsEqual(f1,f2.getRoot()));
	 * 
	 * // false case MultiDirectedGraph<Edge<IPublicNode>,IPublicNode> f3Graph = new
	 * MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>(); //Event start3 = new
	 * Event("start"); //Event end3 = new Event("end"); IPublicNode s22 = new
	 * Send(travelAgency,m3, "send_TA_failure"); f3Graph.addEdge(g12New, s22);
	 * f3Graph.addEdge(s22, g13New); PublicModel f3 = new PublicModel(f3Graph,
	 * "fragment");
	 * 
	 * assertEquals("fragment comparison should return false", false,
	 * FragmentUtil.fragmentIsEqual(f1,f3.getRoot()));
	 * 
	 * // compare two fragments where the recursive nature of the equality check
	 * detects the inequality MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>
	 * f4Graph = new MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>();
	 * 
	 * // declarations Gateway g1New = new AndGateway("g1New"); Gateway g2New = new
	 * AndGateway("g2New"); Gateway g3New = new XorGateway("g3New"); Gateway g4New =
	 * new XorGateway("g4New"); IChoreographyNode a = new Interaction("a", airline,
	 * traveler, m4); IChoreographyNode b = new Interaction("b", traveler,
	 * travelAgency, m5); IChoreographyNode c = new Interaction("c", travelAgency,
	 * traveler, m3); IChoreographyNode d = new Interaction("d", acquirer, airline,
	 * m3);
	 * 
	 * // check complex fragments for equality
	 * MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>
	 * singleActivityDiffGraph = new MultiDirectedGraph<Edge<IChoreographyNode>,
	 * IChoreographyNode>(); singleActivityDiffGraph.addEdge(g1New, g3New);
	 * singleActivityDiffGraph.addEdge(g3New, a);
	 * singleActivityDiffGraph.addEdge(g3New, g4New);
	 * singleActivityDiffGraph.addEdge(g4New, g2New);
	 * singleActivityDiffGraph.addEdge(g1New, b); singleActivityDiffGraph.addEdge(b,
	 * c); singleActivityDiffGraph.addEdge(c, g2New); ChoreographyModel
	 * choreoSingleActivityDiff = new ChoreographyModel(singleActivityDiffGraph,
	 * "fragment 1");
	 * 
	 * Gateway g1New2 = new AndGateway("g1New2"); Gateway g2New2 = new
	 * AndGateway("g2New2"); Gateway g3New2 = new XorGateway("g3New2"); Gateway
	 * g4New2 = new XorGateway("g4New2");
	 * 
	 * MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>
	 * singleActivityDiffGraph2 = new MultiDirectedGraph<Edge<IChoreographyNode>,
	 * IChoreographyNode>(); singleActivityDiffGraph2.addEdge(g1New2, g3New2);
	 * singleActivityDiffGraph2.addEdge(g1New2, g3New2);
	 * singleActivityDiffGraph2.addEdge(g3New2, a);
	 * singleActivityDiffGraph2.addEdge(g3New2, g4New2);
	 * singleActivityDiffGraph2.addEdge(g4New2, g2New2);
	 * singleActivityDiffGraph2.addEdge(g1New2, d);
	 * singleActivityDiffGraph2.addEdge(d, b); singleActivityDiffGraph2.addEdge(b,
	 * c); singleActivityDiffGraph2.addEdge(c, g2New2); ChoreographyModel
	 * choreoSingleActivityDiff2 = new ChoreographyModel(singleActivityDiffGraph2,
	 * "fragment 2");
	 * 
	 * System.out.println("----## Comparing single diff fragments");
	 * 
	 * assertEquals("fragment comparison on single activity difference", false,
	 * FragmentUtil.fragmentIsEqual(choreoSingleActivityDiff.getRoot(),
	 * choreoSingleActivityDiff2.getRoot()));
	 * 
	 * Gateway g5New = new XorGateway("g5New"); Gateway g6New = new
	 * XorGateway("g6New"); Gateway g5New2 = new XorGateway("g5New2"); Gateway
	 * g6New2 = new XorGateway("g6New2");
	 * 
	 * MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>
	 * activityOrderDiffGraph = new MultiDirectedGraph<Edge<IChoreographyNode>,
	 * IChoreographyNode>(); activityOrderDiffGraph.addEdge(g1New, g3New);
	 * activityOrderDiffGraph.addEdge(g3New, a); activityOrderDiffGraph.addEdge(a,
	 * g5New); activityOrderDiffGraph.addEdge(g3New, b);
	 * activityOrderDiffGraph.addEdge(b, g5New);
	 * activityOrderDiffGraph.addEdge(g1New, g4New);
	 * activityOrderDiffGraph.addEdge(g4New, c); activityOrderDiffGraph.addEdge(c,
	 * g6New); activityOrderDiffGraph.addEdge(g4New, d);
	 * activityOrderDiffGraph.addEdge(d, g6New);
	 * activityOrderDiffGraph.addEdge(g5New, g2New);
	 * activityOrderDiffGraph.addEdge(g6New, g2New); ChoreographyModel
	 * choreoActivityOrderDiff = new ChoreographyModel(activityOrderDiffGraph,
	 * "fragment 1");
	 * 
	 * MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>
	 * activityOrderDiffGraph2 = new MultiDirectedGraph<Edge<IChoreographyNode>,
	 * IChoreographyNode>(); activityOrderDiffGraph2.addEdge(g1New2, g3New2);
	 * activityOrderDiffGraph2.addEdge(g3New2, a);
	 * activityOrderDiffGraph2.addEdge(a, g5New2);
	 * activityOrderDiffGraph2.addEdge(g3New2, c);
	 * activityOrderDiffGraph2.addEdge(c, g5New2);
	 * activityOrderDiffGraph2.addEdge(g1New2, g4New2);
	 * activityOrderDiffGraph2.addEdge(g4New2, b);
	 * activityOrderDiffGraph2.addEdge(b, g6New2);
	 * activityOrderDiffGraph2.addEdge(g4New2, d);
	 * activityOrderDiffGraph2.addEdge(d, g6New2);
	 * activityOrderDiffGraph2.addEdge(g5New2, g2New2);
	 * activityOrderDiffGraph2.addEdge(g6New2, g2New2); ChoreographyModel
	 * choreoActivityOrderDiff2 = new ChoreographyModel(activityOrderDiffGraph2,
	 * "framgnet 2");
	 * 
	 * System.out.println("-- activity order fragment equality check");
	 * 
	 * assertEquals("fragment comparison on different activity order", false,
	 * FragmentUtil.fragmentIsEqual(choreoActivityOrderDiff.getRoot(),
	 * choreoActivityOrderDiff2.getRoot()));
	 * 
	 * // null cases
	 * 
	 * assertEquals("comparing null vs null fragments should be equal", true,
	 * FragmentUtil.fragmentIsEqual(null, null));
	 * 
	 * assertEquals("comparing a valid fragment vs null: not equal", false,
	 * FragmentUtil.fragmentIsEqual(choreoActivityOrderDiff.getRoot(), null));
	 * 
	 * assertEquals("comparing null vs a valid fragment: not equal", false,
	 * FragmentUtil.fragmentIsEqual(null, choreoSingleActivityDiff.getRoot())); }
	 * 
	 * //@Test public void changePropagationReplaceInsertTest() {
	 * System.out.println("-- changePropagationReplaceInsertTest()"); // TODO: need
	 * to to make sure collaboration logic is correct : unit tests List<Role> roles
	 * = new LinkedList<Role>(Arrays.asList(traveler, acquirer, travelAgency,
	 * airline)); Collaboration collab = new Collaboration("booktrip");
	 * collab.addPublicModel(acquirer, (PublicModel)pum_acquirer);
	 * collab.addPublicModel(travelAgency, (PublicModel)pum_travelAgency);
	 * collab.addPublicModel(airline, (PublicModel)pum_airline); List<IPrivateModel>
	 * privateModels = new LinkedList<IPrivateModel>(); Choreography
	 * bookTripChoreography = new Choreography("booktripChoreo", collab,
	 * privateModels, (ChoreographyModel)BookTrip);
	 * 
	 * // old fragment IRPSTNode<Edge<IPublicNode>, IPublicNode> f1 =
	 * pum_acquirer.getFragmentBoundedBy(this.g12, this.g13);
	 * 
	 * // TODO: create unit test where the old fragment is null...what happens?
	 * 
	 * // new fragment MultiDirectedGraph<Edge<IPublicNode>,IPublicNode> f2Graph =
	 * new MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>(); Event start = new
	 * Event("start"); Event end = new Event("end"); Gateway g12New = new
	 * AndGateway("g12New"); Gateway g13New = new AndGateway("g13New"); IPublicNode
	 * s11 = new Send(airline,m5, "send_A_failure"); IPublicNode s12 = new
	 * Send(travelAgency,m3, "send_TA_failure"); f2Graph.addEdge(g12New,s11);
	 * f2Graph.addEdge(g12New,s12); f2Graph.addEdge(s11,g13New);
	 * f2Graph.addEdge(s12,g13New); PublicModel f2 = new PublicModel(f2Graph,
	 * "fragment");
	 * 
	 * Replace<Edge<IPublicNode>,IPublicNode> replace = new
	 * Replace<Edge<IPublicNode>,IPublicNode>(f1,f2,pum_acquirer,
	 * bookTripChoreography,acquirer); Map<String, Stats> chgOpId2Stats = new
	 * HashMap<String, Stats>(); Map<Role, List<IChangeOperation>> changeOps =
	 * ChangePropagationUtil.propagate(replace, null, null, chgOpId2Stats);
	 * System.out.println("changeOps: " + changeOps);
	 * 
	 * }
	 * 
	 * @Test public void projectionReturnsReducedGraphTest() { // an empty
	 * projection returns "null" as result.
	 * 
	 * // test example: project travelAgency role in the global choreography //
	 * expected: the resulting projected graph should be equal to the manually //
	 * created public model of travelAgency.
	 * 
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> TA_proj =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.projectionRole(travelAgency);
	 * 
	 * System.out.println("TA projection: " + TA_proj);
	 * 
	 * IOUtils.toFile("projectedTAGraph.dot", TA_proj.getdigraph().toDOT());
	 * IOUtils.toFile("projectedTARPST.dot", TA_proj.toDOT());
	 * 
	 * // case AND BOTH
	 * 
	 * // we manually reduce the graph in this case. We pass "false" as second
	 * parameter RpstModel<Edge<IChoreographyNode>, IChoreographyNode> T_proj =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.projectionRole(traveler, false);
	 * 
	 * // assert: g4 -> g7 is completely deleted as BOTH AND paths are empty
	 * IOUtils.toFile("projectedTGraph_AND_BOTH_Case_g4_to_g7_removed.dot",
	 * T_proj.getdigraph().toDOT());
	 * IOUtils.toFile("projectedTRPST_AND_BOTH_Case_g4_to_g7_removed.dot",
	 * T_proj.toDOT());
	 * 
	 * System.out.println("-- ProjectionReturnsReducedGraphTest()");
	 * 
	 * T_proj = T_proj.reduceGraph();
	 * 
	 * IOUtils.toFile(
	 * "projectedTGraph_AND_BOTH_Case_g4_to_g7_removed_after_reduction.dot",
	 * T_proj.getdigraph().toDOT()); IOUtils.toFile(
	 * "projectedTRPST_AND_BOTH_Case_g4_to_g7_removed_after_reduction.dot",
	 * T_proj.toDOT());
	 * 
	 * this.compareModels(BookTrip, T_proj);
	 * 
	 * assertNodeIdsRemoved("AND BOTH projection case should work", BookTrip,
	 * T_proj, new String[]{ // removed interactions i2.getId(), i3.getId(),
	 * i5.getId(), i6.getId(), i7.getId(), i8.getId(), i9.getId(), i10.getId(),
	 * i11.getId(),
	 * 
	 * // removed gateways g4.getId(), g5.getId(), g6.getId(), g7.getId() });
	 * 
	 * // case AND EITHER
	 * 
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> AI_proj =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.projectionRole(airline, false);
	 * 
	 * IOUtils.toFile(
	 * "projectedAIGraph_AND_EITHER_Case_g4_to_g7_reduced_to_sequence.dot",
	 * AI_proj.getdigraph().toDOT()); IOUtils.toFile(
	 * "projectedAIRPST_AND_EITHER_Case_g4_to_g7_reduced_to_sequence.dot",
	 * AI_proj.toDOT());
	 * 
	 * assertNodeIdsRemoved("AND EITHER projection before reduction.", BookTrip,
	 * AI_proj, new String[] { // removed interactions i1.getId(), i10.getId(),
	 * i11.getId(), i2.getId(), i3.getId(), i4.getId(), i9.getId() // removed
	 * gateways });
	 * 
	 * AI_proj = AI_proj.reduceGraph();
	 * 
	 * IOUtils.toFile(
	 * "projectedAIGraph_AND_EITHER_Case_g4_to_g7_reduced_to_sequence_reduced.dot",
	 * AI_proj.getdigraph().toDOT());
	 * 
	 * // assert: g4 -> g7 should be reduced to sequence, because i9 is gone //
	 * assert: g2 -> g3 stays because it is an XOR EITHER case
	 * assertNodeIdsRemoved("AND EITHER projection after reduction: g4 -> g7 turned to sequence (removed)"
	 * , BookTrip, AI_proj, new String[] { // removed interactions i1.getId(),
	 * i10.getId(), i11.getId(), i2.getId(), i3.getId(), i4.getId(), i9.getId(), //
	 * removed gateways g4.getId(), g7.getId() });
	 * 
	 * // case XOR BOTH
	 * 
	 * // 1.) remove the TA -> traveler "credit card not approved" activity (i4) //
	 * 2.) project BookTrip on traveler. Due to 1.) g1 and g8 is reduced.
	 * 
	 * IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete =
	 * BookTrip.getFragmentWithSourceOrTarget(this.i4);
	 * 
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> m1 =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.delete(node2delete);
	 * 
	 * System.out.println("\n-----AT XOR BOTH case\n");
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> m2 =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * m1.projectionRole(traveler, false);
	 * 
	 * assertNodeIdsRemoved("XOR BOTH projection before reduction", BookTrip, m2,
	 * new String[]{ // removed interactions i2.getId(), i3.getId(), i5.getId(),
	 * i6.getId(), i7.getId(), i8.getId(), i9.getId(), i10.getId(), i11.getId(),
	 * i4.getId(), });
	 * 
	 * IOUtils.toFile("travelerAfterDeleteI4_XOR_BOTH_Case_g1_to_g8.dot",
	 * m2.getdigraph().toDOT());
	 * 
	 * m2 = m2.reduceGraph();
	 * 
	 * assertNodeIdsRemoved("XOR BOTH projection after reduction", BookTrip, m2, new
	 * String[]{ // removed interactions i2.getId(), i3.getId(), i5.getId(),
	 * i6.getId(), i7.getId(), i8.getId(), i9.getId(), i10.getId(), i11.getId(),
	 * i4.getId(),
	 * 
	 * // removed gateways (all) g1.getId(), g2.getId(), g3.getId(), g4.getId(),
	 * g5.getId(), g6.getId(), g7.getId(), g8.getId() });
	 * 
	 * IOUtils.toFile("travelerAfterDeleteI4_XOR_BOTH_Case_g1_to_g8_reduced.dot",
	 * m2.getdigraph().toDOT());
	 * 
	 * // direct XOR BOTH case RpstModel<Edge<IChoreographyNode>, IChoreographyNode>
	 * m3 = (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * m1.projectionRole(traveler);
	 * assertNodeIdsRemoved("XOR Both projection direct reduction", BookTrip, m3,
	 * new String[]{ // removed interactions i2.getId(), i3.getId(), i5.getId(),
	 * i6.getId(), i7.getId(), i8.getId(), i9.getId(), i10.getId(), i11.getId(),
	 * i4.getId(),
	 * 
	 * // removed gateways (all) g1.getId(), g2.getId(), g3.getId(), g4.getId(),
	 * g5.getId(), g6.getId(), g7.getId(), g8.getId() }); }
	 * 
	 * @Test public void fragmentProjectionTest() {
	 * 
	 * System.out.println("#### fragmentProjectionTest"); // projections are done in
	 * the RpstModel context thus far. We want to // proect fragments too.
	 * IRPSTNode<Edge<IPublicNode>, IPublicNode> acquirer_f =
	 * pum_acquirer.getFragmentBoundedBy(this.g12, this.g13);
	 * 
	 * // assert: there are 2 gateways (XOR g12, g13) and 2 activities
	 * (send_A_failure, send_TA_failure)
	 * assertEquals("acquirer_f has the correct number of nodes", 4,
	 * FragmentUtil.collectNodes(acquirer_f).size());
	 * assertEquals("acquirer_f has two gateways", 2,
	 * FragmentUtil.collectGateways(acquirer_f).size());
	 * assertEquals("acquirer_f has two activities", 2,
	 * FragmentUtil.collectActivities(acquirer_f).size());
	 * assertEquals("send_A_failure & send_TA_failure as activities", new
	 * HashSet<IPublicNode>(Arrays.asList(new IPublicNode[]{s11, s12})),
	 * FragmentUtil.collectActivities(acquirer_f));
	 * 
	 * IRPSTNode<Edge<IPublicNode>, IPublicNode> acquirer_f_airline =
	 * FragmentUtil.projectRole(acquirer_f, airline, pum_acquirer.getdigraph(),
	 * true).getRoot(); // assert: f1_airline contains 2 gateways (XOR g12, g13) and
	 * 1 activity (send_A_failure) + 2 events start and end
	 * assertEquals("acquirer_f has the correct number of nodes", 5,
	 * FragmentUtil.collectNodes(acquirer_f_airline).size());
	 * assertEquals("acquirer_f has two gateways", 2,
	 * FragmentUtil.collectGateways(acquirer_f_airline).size());
	 * assertEquals("acquirer_f has two activities", 1,
	 * FragmentUtil.collectActivities(acquirer_f_airline).size());
	 * assertEquals("send_A_failure as activity", new
	 * HashSet<IPublicNode>(Arrays.asList(new IPublicNode[]{s11})),
	 * FragmentUtil.collectActivities(acquirer_f_airline));
	 * 
	 * // get the global fragment of acquirer_f IRPSTNode<Edge<IChoreographyNode>,
	 * IChoreographyNode> f_G =
	 * FragmentUtil.getCorrespondingGlobalFragment(acquirer_f,
	 * (ChoreographyModel)BookTrip);
	 * 
	 * // TA case (use corresponding fragments from acquirer)
	 * IRPSTNode<Edge<IPublicNode>, IPublicNode> ta_f =
	 * FragmentUtil.getCorrespondingLocalFragment(f_G, pum_travelAgency);
	 * 
	 * // assert: there are 2 activities (TA notification failure, CC not approved)
	 * and 2 gateways (TA_g2, TA_g3)
	 * 
	 * System.out.println("#### ta_f.collectActivities: " +
	 * FragmentUtil.collectActivities(ta_f));
	 * 
	 * assertEquals("ta_f has the correct number of nodes", 4,
	 * FragmentUtil.collectNodes(ta_f).size());
	 * assertEquals("ta_f has two gateways", 2,
	 * FragmentUtil.collectGateways(ta_f).size());
	 * assertEquals("ta_f has two activities", 2,
	 * FragmentUtil.collectActivities(ta_f).size());
	 * assertEquals("CC not approved & TA notification failure as activity", new
	 * HashSet<IPublicNode>(Arrays.asList(new IPublicNode[]{ta3, ta4})),
	 * FragmentUtil.collectActivities(ta_f));
	 * 
	 * // project on acquirer IRPSTNode<Edge<IPublicNode>, IPublicNode>
	 * ta_f_acquirer = FragmentUtil.projectRole(ta_f, acquirer,
	 * pum_travelAgency.getdigraph(), true).getRoot();
	 * 
	 * // assert: there are 1 activity (TA notification failure) and 2 gateways
	 * (TA_g2, TA_g3) + 2 events
	 * assertEquals("ta_f_acquirer has the correct number of nodes", 5,
	 * FragmentUtil.collectNodes(ta_f_acquirer).size());
	 * assertEquals("ta_f_acquirer has two gateways", 2,
	 * FragmentUtil.collectGateways(ta_f_acquirer).size());
	 * assertEquals("ta_f_acquirer has two activities", 1,
	 * FragmentUtil.collectActivities(ta_f_acquirer).size());
	 * assertEquals("TA notification failure as activity", new
	 * HashSet<IPublicNode>(Arrays.asList(new IPublicNode[]{ta3})),
	 * FragmentUtil.collectActivities(ta_f_acquirer));
	 * 
	 * }
	 * 
	 * //@Test public void testInsertOperationIsNonDestructive() { // we make sure
	 * that the insert operation is non-destructive, and that // it returns a new
	 * instance with the modified graph
	 * 
	 * // new fragment with single interaction -> we add it after the existing i2
	 * MultiDirectedGraph<Edge<IChoreographyNode>, IChoreographyNode>
	 * newFragmentGraph = new MultiDirectedGraph<Edge<IChoreographyNode>,
	 * IChoreographyNode>(); IChoreographyNode iNew = new
	 * Interaction("INew",traveler,acquirer, this.m2); Event e1 = new
	 * Event("start"); Event e2 = new Event("end"); newFragmentGraph.addEdge(e1,
	 * iNew); newFragmentGraph.addEdge(iNew, e2); RpstModel<Edge<IChoreographyNode>,
	 * IChoreographyNode> B2 = new ChoreographyModel(newFragmentGraph);
	 * 
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter =
	 * BookTrip.insert(B2.getRoot(), BookTrip.getdigraph().getEdge(this.i2,
	 * this.g1));
	 * 
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> projWithoutINew =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.projectionRole(traveler); RpstModel<Edge<IChoreographyNode>,
	 * IChoreographyNode> projWithINew = (RpstModel<Edge<IChoreographyNode>,
	 * IChoreographyNode>) BookTripAfter.projectionRole(traveler);
	 * 
	 * this.assertNodeIdsRemoved("iNew should NOT occur in projWithoutINew",
	 * projWithINew, projWithoutINew, new String[]{iNew.getId()});
	 * this.assertNodeIdsAdded("iNew should occur in projWithINew", projWithoutINew,
	 * projWithINew, new String[]{iNew.getId()});
	 * this.assertNodeExists("iNew should occur after insertion in projWithINew",
	 * projWithINew, iNew); }
	 * 
	 * // @Test public void testDeleteOperationIsNonDestructive() { // make sure
	 * that the delete operation is non-destructive
	 * 
	 * IRPSTNode<Edge<IChoreographyNode>, IChoreographyNode> node2delete =
	 * BookTrip.getFragmentWithSourceOrTarget(this.i4);
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTripAfter =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.delete(node2delete);
	 * 
	 * RpstModel<Edge<IChoreographyNode>, IChoreographyNode> projOriginal =
	 * (RpstModel<Edge<IChoreographyNode>, IChoreographyNode>)
	 * BookTrip.projectionRole(traveler); RpstModel<Edge<IChoreographyNode>,
	 * IChoreographyNode> projAfterDelete = (RpstModel<Edge<IChoreographyNode>,
	 * IChoreographyNode>) BookTrip.projectionRole(traveler);
	 * 
	 * this.assertNodeExists("i4 should still exist in projOriginal", projOriginal,
	 * i4); }
	 * 
	 * // @Test public void testReplaceOperationIsNonDestructive() { //Creating a
	 * new fragment MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>
	 * newFragmentgraph = new MultiDirectedGraph<Edge<IPublicNode>,IPublicNode>();
	 * Event e1 = new Event("start"); Event e2 = new Event("end"); IPublicNode s11 =
	 * new Send(airline,m5, "send_A_failure"); IPublicNode s12 = new
	 * Send(travelAgency,m3, "send_TA_failure"); newFragmentgraph.addEdge(e1,s11);
	 * newFragmentgraph.addEdge(s11,s12);newFragmentgraph.addEdge(s12,e2);
	 * PublicModel fragment = new PublicModel(newFragmentgraph, "fragment");
	 * 
	 * // replace with the old fragment IRPSTNode<Edge<IPublicNode>, IPublicNode>
	 * node2replace = pum_acquirer.getFragmentWithSourceOrTarget(this.g12);
	 * RpstModel<Edge<IPublicNode>, IPublicNode> pum_acquirer_after =
	 * pum_acquirer.replace(node2replace,fragment.getRoot());
	 * 
	 * // assert that g12 and g13 are gone in the replaced graph
	 * assertEquals("g12 should be gone now", null,
	 * pum_acquirer_after.getFragmentWithSourceOrTarget(this.g12));
	 * assertEquals("g13 should be gone as well", null,
	 * pum_acquirer_after.getFragmentWithSourceOrTarget(this.g13));
	 * 
	 * // assert that the new fragment contains s11, s12, as well as g11 and g16
	 * IRPSTNode<Edge<IPublicNode>, IPublicNode> newFragment =
	 * pum_acquirer_after.getFragmentWithSourceOrTarget(s11); Set<String>
	 * expectedIds = new HashSet<String>(Arrays.asList(new String[]{s11.getId(),
	 * s12.getId(), this.g11.getId(), this.g16.getId()}));
	 * assertEquals("The new fragment should contain s11,s12,g11 and g16",
	 * expectedIds, FragmentUtil.collectNodeIds(newFragment));
	 * 
	 * IOUtils.toFile("ReplaceNonDestructiveBefore.dot",
	 * pum_acquirer.getdigraph().toDOT());
	 * IOUtils.toFile("ReplaceNonDestructiveAfter.dot",
	 * pum_acquirer_after.getdigraph().toDOT());
	 * 
	 * System.out.println("Comparing pum_acquirer_after -> pum_acquirer");
	 * this.compareModels(pum_acquirer_after, pum_acquirer);
	 * 
	 * this.assertNodeIdsAddedAndRemoved(
	 * "Make sure that the original model is intact", pum_acquirer_after,
	 * pum_acquirer, new String[]{ this.s11.getId(), this.s12.getId(),
	 * this.g12.getId(), this.g13.getId() }, // added new String[]{ s11.getId(),
	 * s12.getId() } );
	 * 
	 * }
	 */
	@Test
	public void testNegotiationSimulationData() throws IOException {
		NegotiationSimulation negosim = new NegotiationSimulation(this.bookTripChoreography, 3, 1000);
	}

}
