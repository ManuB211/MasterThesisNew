package at.ac.c3pro.chormodel.generation;

import java.io.IOException;
import java.util.logging.*;

public class GenerationLogger {
    static private FileHandler fileTxt;
    static private SimpleFormatter formatterTxt;

    static public void setup() throws IOException {

        // get the global logger to configure it
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        logger.setLevel(Level.INFO);
        fileTxt = new FileHandler("results/Logging.txt");

        // create a TXT formatter
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);

    }

    private static class MyCustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            // sb.append("Prefixn");
            String sb = record.getMessage() +
                    "Suffixn" +
                    "n";
            return sb;
        }
    }
}
