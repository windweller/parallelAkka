package classification;

import FolderReadingNIO.CSVHandler;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

/**
 * This class reads in CSV file and
 * transform it into Mallet compatible
 * text file with three columns
 * type T is the criteria type when you split
 * data
 */
public class CSV {

    private File file;
    private int idColumn;
    private int sentColumn;
    private File outputFile;
    private int criteriaCol;

    public CSV(String fileLoc, String outputFile, int idColumn, int sentColumn, int criteriaCol) {
        this.file = new File(fileLoc);
        this.outputFile = new File(outputFile);
        this.idColumn = idColumn;
        this.sentColumn = sentColumn;
        this.criteriaCol = criteriaCol;
    }

    public void transform(Function<Integer, String> func) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file));

        FileWriter fw = new FileWriter(outputFile);

        CSVPrinter csvFilePrinter = new CSVPrinter(fw, CSVFormat.RFC4180);

        String line = br.readLine();
        line = br.readLine(); // skip header

        while (line != null) {
            ArrayList<String> parts = CSVHandler.parseLine(line);
            csvFilePrinter.printRecord(Arrays.asList(parts.get(idColumn), func.apply(Integer.parseInt(parts.get(criteriaCol))), parts.get(sentColumn)));

            line = br.readLine();
        }

        fw.flush();
        csvFilePrinter.flush();
        fw.close();
        csvFilePrinter.close();
    }
}
