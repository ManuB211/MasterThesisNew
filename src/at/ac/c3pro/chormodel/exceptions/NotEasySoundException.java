package at.ac.c3pro.chormodel.exceptions;

public class NotEasySoundException extends Exception {

    /**
     * Needed due to the fact that there is an instance rooted in the message flow logic, that might assign two HOW-receives to the same participant (see usages)
     * In instances like this, an exception is thrown to restart the generation algorithm.
     */
    public NotEasySoundException() {
        super();

        System.err.println("After the Easy-Soundness Check & Repair routine, no subgraphs are left that are easy-sound");
    }
}
