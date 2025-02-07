package at.ac.c3pro.util;

import java.io.IOException;

public class VisualizationHandler {

    public static void visualize(VisualizationType type) throws InterruptedException, IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("python", type.getPath(), GlobalTimestamp.timestamp, type.getFolderName());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        process.waitFor();
    }

    public enum VisualizationType {
        PETRI_NET("resources/generatePetrinetVisualization.py", ""),
        PRIV_MODEL("resources/generateDotVisualization.py", "PrivateModels"),
        PUB_MODEL("resources/generateDotVisualization.py", "PublicModels");

        private String path;
        private String folderName;

        VisualizationType(String path, String folderName) {
            this.path = path;
            this.folderName = folderName;
        }

        String getPath() {
            return path;
        }

        String getFolderName() {
            return folderName;
        }

    }

}
