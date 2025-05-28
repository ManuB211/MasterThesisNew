package at.ac.c3pro.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class OutputHandler {

    private PrintWriter printWriter;
    private DebugLevel debugLevel;

    public OutputHandler(OutputType type, DebugLevel debugLevel) throws IOException {
        createOutputFolder(type.getFolderName());
        this.printWriter = new PrintWriter("target/" + GlobalTimestamp.timestamp + "/" + type.getFolderName() + "/" + type.getFileName());
        this.debugLevel = debugLevel;
    }

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

    public void printEasySoundness(String toWrite, DebugLevel dLevel) {
        if (dLevel.getLevel() <= debugLevel.getLevel()) {
            printWriter.println(toWrite);
            printWriter.flush();
            System.out.println(toWrite);
        }
    }

    public void printEasySoundness(EasySoundnessAnalyisBlocks toWrite) {
        printWriter.println(toWrite.getContent());
        printWriter.flush();
        System.out.println(toWrite.getContent());
    }

    public void closePrintWriter() {
        this.printWriter.close();
    }


    /**
     * For the instantiation of the output handler
     * TODO: Add all possibilities of debug outputs that might be interesting to log
     */
    public enum OutputType {
        //        EASY_SOUNDNESS("target" + GlobalTimestamp.timestamp + "/EasySoundness/Analysis.txt")
        EASY_SOUNDNESS("EasySoundness", "Analysis.txt");


        private final String folderName;
        private final String filename;

        OutputType(String folderName, String filename) {
            this.folderName = folderName;
            this.filename = filename;
        }

        public String getFolderName() {
            return folderName;
        }

        public String getFileName() {
            return filename;
        }
    }

    /**
     * Boilerplate Printings for EasySoundnessAnalysis
     */
    public enum EasySoundnessAnalyisBlocks {
        START_CYCLIC_WAITS("++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\nBegin Check for cyclic waits\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++"),
        STOP_CYCLIC_WAITS("++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\nEnd Check for cyclic waits\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++"),
        START_VALID_TRACES("++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\nBegin Check for valid traces\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++"),
        STOP_VALID_TRACES("++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\nEnd Check for valid traces\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++"),
        TRACE_DELIM("-----------------------------------------------------------------------------------\n"),
        NODE_DELIM("-----------------------------------"),
        INTERACTIONS_TO_CHECK_DELIM("\n<><><><><><><><><><><><><><><><><><><><>\n");

        private final String content;

        EasySoundnessAnalyisBlocks(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    //To handle how much is actually printed to the file, so that it does not become 15Gb
    public enum DebugLevel {
        INFO(1),
        DEBUG(2);

        private final int level;

        DebugLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public static DebugLevel getLevelByValue(int value) {
            if (value == 1) {
                return INFO;
            } else if (value == 2) {
                return DEBUG;
            } else {
                throw new IllegalArgumentException("No DebugLevel " + value + " found!");
            }
        }
    }
}
