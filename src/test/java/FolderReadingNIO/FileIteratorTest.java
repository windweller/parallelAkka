package FolderReadingNIO;

import org.junit.Test;

import static org.junit.Assert.*;

public class FileIteratorTest {

    @Test
    public void testNextLine() throws Exception {
        FileIterator fi = new FileIterator("E:\\Allen\\NYTFuture\\NYT");
        while (fi.hasNext())
            fi.nextLine();
    }
}