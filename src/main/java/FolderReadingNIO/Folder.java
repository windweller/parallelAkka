package FolderReadingNIO;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


public class Folder {

    /**
     * Use NIO to read a list of files
     * @param loc the parent folder address
     */
    public static ArrayList<String> getFilePaths(String loc) {

        ArrayList<String> fileNames = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(loc))) {
            for (Path path : directoryStream) {
                fileNames.add(path.toString());
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return fileNames;
    }

}
