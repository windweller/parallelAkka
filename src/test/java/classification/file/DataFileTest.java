package classification.file;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DataFileTest {

    @Test
    public void testCreate() throws Exception {
        DataFile df = DataFile.create(FileType.CSVFile, 0, 0, false);
        df.content = new ArrayList<>();

        df.content.add(new ArrayList<>(Arrays.asList("a", "123", "123")));
        df.content.add(new ArrayList<>(Arrays.asList("b", "456", "456")));
        df.content.add(new ArrayList<>(Arrays.asList("c", "789", "789")));

        df.dropCols(2);

        DataFile df2 = DataFile.create(FileType.CSVFile, 0, 0, false);
        df2.content = new ArrayList<>();

        df2.content.add(new ArrayList<>(Arrays.asList("a")));
        df2.content.add(new ArrayList<>(Arrays.asList("b")));
        df2.content.add(new ArrayList<>(Arrays.asList("c")));

        DataTable dt = DataFile.merge(df, df2);

        dt.content.entrySet().stream().forEach(k -> System.out.println(k.getKey() + " : " + k.getValue()));
    }

    @Test
    public void testListToArrayList() throws Exception {
        List<String> vectors = new ArrayList<>();
        String line = "this is a b c d e";
        vectors = Arrays.asList(line.split(" "));
        ArrayList<ArrayList<String>> content = new ArrayList<>();
        content.add(new ArrayList<>(vectors));

        System.out.println(content.get(0).get(0));

    }
}