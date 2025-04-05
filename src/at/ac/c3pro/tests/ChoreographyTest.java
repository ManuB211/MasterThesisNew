package at.ac.c3pro.tests;

//import at.ac.c3pro.ChangeNegotiation.NegotiationSimulation;

import at.ac.c3pro.chormodel.*;
import at.ac.c3pro.node.*;
import at.ac.c3pro.util.FragmentUtil;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ChoreographyTest {

    // RPST of choreography model
    private RpstModel<Edge<IChoreographyNode>, IChoreographyNode> BookTrip;
    private RpstModel<Edge<IPublicNode>, IPublicNode> pum_acquirer;
    private RpstModel<Edge<IPublicNode>, IPublicNode> pum_travelAgency;
    private RpstModel<Edge<IPublicNode>, IPublicNode> pum_airline;
    private RpstModel<Edge<IPublicNode>, IPublicNode> pum_traveler;
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
//        this.bookTripChoreography = new Choreography("booktripChoreo", collab, privateModels,
//                (ChoreographyModel) BookTrip);

    }

    @Before
    public void setUp() {
        this.declareCommon();
        this.buildChoreographyModel();
        this.buildPublicAcquirerModel();
//        this.buildPrivateAcquirerModel();
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
                new String[]{iNew.getId()});

        // we make sure that the insert operation is non-destructive, and that
        // it returns a new instance with the modified graph
        this.assertNodeIdsRemoved("iNew should not exist in the original instance", BookTripAfter, BookTrip,
                new String[]{iNew.getId()});
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
        assertNull("g12 should be gone now", pum_acquirer.getFragmentWithSourceOrTarget(this.g12));
        assertNull("g13 should be gone as well", pum_acquirer.getFragmentWithSourceOrTarget(this.g13));

        // assert that the new fragment contains s11, s12, as well as g11 and g16
        IRPSTNode<Edge<IPublicNode>, IPublicNode> newFragment = pum_acquirer.getFragmentWithSourceOrTarget(s11);
        Set<String> expectedIds = new HashSet<String>(
                Arrays.asList(s11.getId(), s12.getId(), this.g11.getId(), this.g16.getId()));
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
        assertNotNull(pum_acquirer_after.getFragmentWithSourceOrTarget(this.g12));
        assertNotNull(pum_acquirer_after.getFragmentWithSourceOrTarget(this.g13));

        // assert that the same node ids are there
        this.assertNodeIdsRemoved("The identical replace should result in an intact graph:", pum_acquirer,
                pum_acquirer_after, new String[]{});
    }
}
