package classification.file;

import java.io.*;
import java.util.*;

/**
 * I decide to use a Continuous Passing Style
 * DataFile.create("\t", 1, false).readIn("...\..\..").dropCols(1,2,3,4)
 *
 * DataFile.merge(file1, file2)
 */
public class DataFile {

    FileType ft;
    Boolean header;
    public ArrayList<ArrayList<String>> content = new ArrayList<>();
    public int idColumn;
    public int targetColumn;

    private DataFile(int targetColmun) {this.targetColumn = targetColmun;}

    public static DataFile create(FileType ft, int idColumn, int targetColumn, Boolean header) {
        DataFile dataFile = new DataFile(targetColumn);
        dataFile.ft = ft;
        dataFile.header = header;
        dataFile.idColumn = idColumn;
        return dataFile;
    }

    public static DataFile create() {
        return new DataFile(-1);
    }

    public DataFile readIn(String addr) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(addr)));
        String line = br.readLine();
        if (header) line = br.readLine();
        while (line != null) {
            List<String> vectors = new ArrayList<String>();
            switch (ft) {
                case TabFile:
                    vectors = Arrays.asList(line.split(ft.getSep()));
                    break;
                case CSVFile:
                    vectors = CSVHandler.parseLine(line);
                    break;
                default:
                    System.out.println("must provide a file type: CSV or Tab");
                    System.exit(0);
            }

            content.add(new ArrayList<>(vectors));
            line = br.readLine();
        }
        return this;
    }

    //drop multiple columns
    //targetColumn will change when you are dropping columns
    public DataFile dropCols(int... columns) {

        ArrayList<Integer> columnsToDrop = new ArrayList<>();

        int cul = 0;
        for (int i : columns) {
            if (i < targetColumn)
                cul++; //shrink targetColumn
            if (i < idColumn)
                idColumn--;
            columnsToDrop.add(i);
        }

        targetColumn = targetColumn - cul;

        Collections.sort(columnsToDrop, Comparator.reverseOrder());

        for (int i : columnsToDrop) {
            content.forEach(row -> row.remove(i));
        }

        return this;
    }

    public DataTable toDataTable() {

        int newTargetColumn = targetColumn;
        if (this.targetColumn > this.idColumn)
            newTargetColumn--;

        DataTable dt = new DataTable(newTargetColumn);

        this.content.forEach(row -> dt.put(row.remove(this.idColumn), row));

        return dt;
    }

    /**
     * In order not to distort any previous data file
     * this method is static
     * A first, B later
     * it will take A's targetColumn by default
     * If you want to manually change target, just change fileA.targetColumn
     * It will drop B's targetColumn
     */
    public static DataTable merge(DataFile dataFileA, DataFile dataFileB) {
        //get two arraylists, based on common column

        Map<String, ArrayList<String>> mapA = new HashMap<>();
        dataFileA.content.forEach(row -> {
            mapA.put(row.remove(dataFileA.idColumn), row);
        });


        Map<String, ArrayList<String>> mapB = new HashMap<>();
        dataFileB.content.forEach(row -> {
            if (dataFileB.targetColumn != -1)
                row.remove(dataFileB.targetColumn);
            mapB.put(row.remove(dataFileB.idColumn), row);
        });

        DataTable dt = new DataTable();

        if (mapA.size() > mapB.size()) {
           mapB.entrySet().stream().forEach(entry -> {
               if (entry.getValue() == null)
                   entry.setValue(new ArrayList<String>());
                entry.getValue().addAll(mapA.get(entry.getKey()));
           });
            if (dataFileA.idColumn < dataFileA.targetColumn)
                dataFileA.targetColumn--; //because you dropped id
            dt.targetColumn = dataFileA.targetColumn;
            dt.content = mapB;
        }
        else {
            mapA.entrySet().stream().forEach(entry -> {
                if (entry.getValue() == null)
                    entry.setValue(new ArrayList<String>());
                entry.getValue().addAll(mapB.get(entry.getKey()));
            }); //we append

           if (dataFileA.idColumn < dataFileA.targetColumn)
               dataFileA.targetColumn--; //because you dropped id
           dt.targetColumn = dataFileA.targetColumn;
           dt.content = mapA;
        }

        return dt;
    }

    public static DataFile combine(DataFile dataFileA, DataFile dataFileB) {
        if (dataFileA.content.get(0).size() != dataFileB.content.get(0).size()) {
            System.out.println("column number isn't the same");
            System.exit(0);
        }
        else if (dataFileA.targetColumn != dataFileB.targetColumn) {
            System.out.println("target column isn't the same");
            System.exit(0);
        }

        //append B under A
        dataFileB.content.forEach(dataFileA.content::add);

        return dataFileA;
    }

    public DataFile append(DataFile dataFileB) {
        if (content.get(0).size() != dataFileB.content.get(0).size()) {
            System.out.println("column number isn't the same");
            System.exit(0);
        }
        else if (targetColumn != dataFileB.targetColumn) {
            System.out.println("target column isn't the same");
            System.exit(0);
        }

        dataFileB.content.forEach(this.content::add);

        return this;
    }

}
