package classification.file;

import cc.mallet.types.Instance;

import java.util.*;

/**
 * Created by anie on 3/4/2015.
 */
public class DataTable {

    //String is the id
    public Map<String, ArrayList<String>> content;
    public ArrayList<String> targets = new ArrayList<>();
    public int targetColumn;
    public int columnSize = 0;

    public DataTable() {
        content = new HashMap<>();
        this.targetColumn = -1; //meaning there is no target
    }

    public DataTable(int targetColumn) {
        content = new HashMap<>();
        this.targetColumn = targetColumn;
    }

    public DataTable(Map<String, ArrayList<String>> content, int targetColumn) {
        this.content = content;
        this.targetColumn = targetColumn;
        populateTargets(content, targetColumn);
    }

    public void put(String key, ArrayList<String> value) {
        this.columnSize = value.size();
        content.put(key, value);
    }

    public void calcColumnSize() {
        int count = 0;
        if (content != null)
            for (Map.Entry<String, ArrayList<String>> en : content.entrySet()) {
            count = en.getValue().size();
            break;
        }
        columnSize = count;
    }

    private void populateTargets(Map<String, ArrayList<String>> content, int targetColumn) {

        content.entrySet().stream().forEach(e -> {columnSize++; targets.add(e.getValue().get(targetColumn));});
    }

    public static DataTable instanceToDataTable(Instance instance, Boolean includeData) {
        DataTable dt = new DataTable();
        if (!includeData) {
            dt.content.put((String) instance.getName(), new ArrayList<>());
        }
        else{
            System.out.println("data inclusion hasn't been implemented yet.");
            System.exit(0);
        }
        return dt;
    }

    public DataTable dropCols(int... columns) {

        ArrayList<Integer> columnsToDrop = new ArrayList<>();

        int cul = 0;
        for (int i : columns) {
            if (i < targetColumn)
                cul++; //shrink targetColumn
            columnsToDrop.add(i);
            columnSize--;
        }

        targetColumn -= cul;
        Collections.sort(columnsToDrop, Comparator.reverseOrder());

        for (int i : columnsToDrop) {
            content.entrySet().stream().forEach(row -> row.getValue().remove(i));
        }

        return this;
    }

    // merge will always drop dt2's target and go with dt1
    public static DataTable merge(DataTable dt1, DataTable dt2) {
        DataTable dt = new DataTable();

        if (dt2.targetColumn >= 0) {
            dt2.content.entrySet().stream().forEach(e -> e.getValue().remove(dt2.targetColumn));
            dt2.columnSize--;
        }

        if (dt1.content.size() > dt2.content.size()) {
            int columnSize = 0;
            dt2.content.entrySet().stream().forEach(entry -> {
                entry.getValue().addAll(dt1.content.get(entry.getKey()));
                dt.content.put(entry.getKey(), entry.getValue());
            });
            dt.targetColumn = dt2.columnSize + dt1.targetColumn; //should help? Should be correct by now?? Need testing
        }
        else {
            dt1.content.entrySet().stream().forEach(entry -> {
                entry.getValue().addAll(dt2.content.get(entry.getKey()));
                dt.content.put(entry.getKey(), entry.getValue());
            });
            dt.targetColumn = dt1.targetColumn;
        }

        return dt;
    }


    public static DataTable merge(DataTable dt1, DataFile df2) {
        DataTable dt = new DataTable();

        Map<String, ArrayList<String>> map2 = new HashMap<>();
        df2.content.forEach(row -> map2.put(row.remove(df2.idColumn), row));

        if (dt1.content.size() > map2.size()) {
            map2.entrySet().stream().forEach(entry -> {
                entry.getValue().addAll(dt1.content.get(entry.getKey()));
            });
            dt.content = map2;
        }
        else {
            dt1.content.entrySet().stream().forEach(entry -> {
                entry.getValue().addAll(map2.get(entry.getKey()));
                dt.content.put(entry.getKey(), entry.getValue());
            });
        }

        return dt;
    }
}
