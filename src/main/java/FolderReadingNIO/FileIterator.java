package FolderReadingNIO;

import scala.Array;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by anie on 3/14/2015.
 */
public class FileIterator {

    private Iterator<File> remaining;
    private File currentFile;
    private Iterator<String> currentFileIterator;

    public FileIterator(String dir) throws FileNotFoundException {
        ArrayList<File> files = getFiles(dir);
        this.remaining = files.iterator();
        currentFile = remaining.next();
        currentFileIterator = new BufferedReader(new FileReader(currentFile)).lines().iterator();
    }

    private ArrayList<File> getFiles(String dir) {
        File dire = new File(dir);
        if (dire.isDirectory()) {
            ArrayList<File> files = new ArrayList<>();
            File[] rawfiles = dire.listFiles();
            assert rawfiles != null;
            Collections.addAll(files, rawfiles);
            return files;
        }
        else return new ArrayList<File>(Arrays.asList(dire));
    }

    public String nextLine() throws FileNotFoundException {
        if (currentFileIterator.hasNext())
            return currentFileIterator.next();
        else {
            currentFile = remaining.next();
            currentFileIterator = new BufferedReader(new FileReader(currentFile)).lines().iterator();
            return nextLine();
        }
    }

    public boolean hasNext() {
        return remaining.hasNext() || currentFileIterator.hasNext();
    }
}
