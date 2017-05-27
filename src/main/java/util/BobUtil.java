package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BobUtil {

    /**
     * Returns false if the output could not be written for any reason.
     * @param text
     * @param filePath
     * @param append
     * @return
     */
    public static boolean writeTextToFile(String text, String filePath, boolean append) {
        File file = new File(filePath);
        boolean fileCreated = false;

        //Check for any missing directories and silently create them if we can
        if (!file.getParentFile().exists()) {
            boolean directoriesCreated = file.getParentFile().mkdirs();
            if (!directoriesCreated) return false;
        }

        //Check if the output file exists and create it if we can
        if (!file.exists()) {
            try {
                fileCreated = file.createNewFile();
                if (!fileCreated) return false;
            } catch (IOException e) {
                e.printStackTrace(); return false;
            }
        }

        //now try writing the output string
        try(FileWriter writer = new FileWriter(filePath, append)) {
            if (fileCreated) writer.write(text);
            else writer.write(System.lineSeparator() + text);
            return true;
        } catch (IOException e) {
            System.out.println("Could not save text to: " + filePath + " - TEXT: " + text);
            e.printStackTrace();
            return false;
        }
    }
}
