package at.ac.c3pro.chormodel.exceptions;

public class NoTracesToEndFoundException extends Exception {

    public NoTracesToEndFoundException() {
        super();

        System.err.println("The method to find all traces to the end returned null.");
    }
}
