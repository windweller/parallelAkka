package classification.file;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class DataTableTest {

    @Test
    public void testMerge() throws Exception {
        DataTable dt1 = new DataTable(2);
        dt1.put("a", new ArrayList<>(Arrays.asList("123", "123", "Future", "123")));
        dt1.put("b", new ArrayList<>(Arrays.asList("123", "123", "Future", "123")));
        dt1.put("c", new ArrayList<>(Arrays.asList("123", "123", "Future", "123")));
//        dt1.put("d", new ArrayList<>(Arrays.asList("123", "123", "Future", "123")));

        DataTable dt2 = new DataTable();
        dt2.put("a", new ArrayList<>(Arrays.asList("456", "456")));
        dt2.put("b", new ArrayList<>(Arrays.asList("456", "456")));
        dt2.put("c", new ArrayList<>(Arrays.asList("456",  "456")));
        dt2.put("d", new ArrayList<>(Arrays.asList("456",  "456")));

        dt2.dropCols(0);
        dt1.dropCols(1);

//        dt1.content.entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + " : "  + entry.getValue()));
//        dt2.content.entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + " : " + entry.getValue()));

//        System.out.println(dt1.targetColumn);
//        System.out.println(dt2.targetColumn);
//
//        System.out.println(dt1.columnSize);
//        System.out.println(dt2.columnSize);

        DataTable dt3 = DataTable.merge(dt1, dt2);

        dt3.content.entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + " : " + entry.getValue().get(dt3.targetColumn) + entry.getValue()));

    }
}