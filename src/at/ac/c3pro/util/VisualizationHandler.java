package at.ac.c3pro.util;

import java.io.IOException;

public class VisualizationHandler {

    public static void visualize(VisualizationType type) throws InterruptedException, IOException {

        ProcessBuilder processBuilder;

        if (type.getFolderName().isEmpty()) {
            processBuilder = new ProcessBuilder("python", type.getPath(), GlobalTimestamp.timestamp, type.getFileName());
        } else {
            processBuilder = new ProcessBuilder("python", type.getPath(), GlobalTimestamp.timestamp, type.getFolderName());
        }

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        process.waitFor();
    }

    public enum VisualizationType {
        PETRI_NET("resources/generatePetrinetVisualization.py", "CPNs_private", ""),
        PRIV_MODEL("resources/generateDotVisualization.py", "PrivateModels", ""),
        PUB_MODEL("resources/generateDotVisualization.py", "PublicModels", ""),
        FINISHED_GRAPH_ENRICHED("resources/generateDotVisualizationSingleFile.py", "", "finished_graph_enriched.dot");

        private String path;
        private String folderName;
        private String fileName;

        VisualizationType(String path, String folderName, String fileName) {
            this.path = path;
            this.folderName = folderName;
            this.fileName = fileName;
        }

        String getPath() {
            return path;
        }

        String getFolderName() {
            return folderName;
        }

        String getFileName() {
            return fileName;
        }

    }

}
