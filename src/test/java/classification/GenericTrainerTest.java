package classification;

import cc.mallet.types.InstanceList;
import classification.file.DataFile;
import classification.file.DataTable;
import classification.file.FileType;
import classification.subtree.SubtreePipeBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

public class GenericTrainerTest {

    @Test
    public void testCombineFiles() throws Exception {

        String trainFile = "E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\" + "nfold_"+ 1 + "_train.txt";
        String testFile = "E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\" + "nfold_"+ 1 + "_test.txt";

        DataFile train = DataFile.create(FileType.TabFile, 0, 1, false).readIn(trainFile);
        DataFile test = DataFile.create(FileType.TabFile, 0, 1, false).readIn(testFile);

        DataFile sum = train.append(test);

        DataFile features = DataFile.create(FileType.CSVFile, 1, 67, true).readIn("E:\\Allen\\R\\naacl2015\\MTurkAllSentences_Processed.csv");

        features.dropCols(0, 2, 3, 4, 66, 65, 64, 63);

//        features.content.get(0).forEach(e -> System.out.println(e + "\t"));
//        System.out.println("target column is: " + features.content.get(0).get(features.targetColumn));
//        System.out.println("id column is: " + features.content.get(0).get(features.idColumn));


//        sum.content.forEach(e -> {e.forEach(f -> System.out.print(f + "\t")); System.out.println();});

        DataTable sumWithFeaturs = DataFile.merge(sum, features);
//
//        sumWithFeaturs.content.entrySet().stream().forEach(e -> System.out.println(e.getKey() + " target is " + e.getValue().get(sumWithFeaturs.targetColumn) + e.getValue()));

//        sumWithFeaturs.content.entrySet().stream().forEach(e -> System.out.println(e.getValue().size()));

        InstanceList instances = new SubtreePipeBuilder(0, sumWithFeaturs.targetColumn, false, false).readingFromDataTable(sumWithFeaturs);
    }
}