package at.ac.c3pro.chormodel.exceptions;

public class TwoHOWReceiveForOneParticipantException extends Exception {

    /**
     * Needed due to the fact that there is an instance rooted in the message flow logic, that might assign two HOW-receives to the same participant (see usages)
     * In instances like this, an exception is thrown to restart the generation algorithm.
     */
    public TwoHOWReceiveForOneParticipantException() {
        super();

        System.err.println("Message Flow algorithm would've been forced to assign an additional HOW receive to a participant that already has one");
    }
}
