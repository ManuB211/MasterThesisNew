package at.ac.c3pro.util;

import java.io.File;
import java.io.IOException;

public class OutputHandler {

    /**
     * Creates an output folder with the given name. If the name is null it creates the target folder itself
     */
    public static File createOutputFolder(String name) throws IOException {
        File dir = new File("target/" + GlobalTimestamp.timestamp + (name != null ? "/" + name : ""));

        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (created) {
                System.out.println("Directory created successfully!");
            } else {
                throw new IOException("Failed to create the directory.");
            }
        } else {
            System.err.println("Directory already exists.");
        }

        return dir;
    }
}