package at.ac.c3pro.chormodel.exceptions;

public class PrivateModelDisconnectedException extends Exception {

    public PrivateModelDisconnectedException(String roleName, String errorMessage) {
        super(errorMessage);

        System.err.println("Public Model of Participant " + roleName + " was disconnected. Starting new generation approach");
    }
}
