package classification.subtree.varro;

import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import classification.file.DataFile;
import classification.file.DataTable;
import classification.file.FileType;
import classification.subtree.RulesTrainer;
import classification.subtree.SubtreeTrainer;
import classification.svm.SVMTrainer;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VarroXMLReaderTest {

    //        reader.getSubtreeList().entrySet().stream().forEach(e -> System.out.println(e.getKey() + " : " + e.getValue()));
//        System.out.println();
//        reader.getSubtreeSentenceMap().entrySet().stream().forEach(e -> System.out.println(e.getKey() + " : " + e.getValue()));

    @Test
    public void testParse() throws Exception {
        long st = System.currentTimeMillis();

        VarroXMLReader reader = new VarroXMLReader();

        reader.parse(new int[] {867, 1140}, "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesFuture.xml",
                "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesNANFuture.xml");

        reader.process(0.001, true);

        System.out.println(reader.top.size());
//        System.out.println(reader.bottom.size());

//        System.out.println(reader.sentenceVector.size());

        DataTable sentenceFeatures = new DataTable();
        sentenceFeatures.content = reader.sentenceVector;
        sentenceFeatures.calcColumnSize(); //this is crucial!!!!!!!!

        startClassification(sentenceFeatures);

        long et = System.currentTimeMillis();

        System.out.println("done!");

//        System.out.println(reader.getSubtreeList().size());

        System.out.println("time used: " + (et - st));

    }

    @Test
    public void testReplaceID() throws Exception {
        long st = System.currentTimeMillis();

        DataFile originalF = DataFile.create(FileType.CSVFile, 1, 67, true).readIn("E:\\Allen\\R\\naacl2015\\MTurkAllSentences_Processed.csv");
        originalF.dropCols(0, 2, 3, 4, 66, 65, 64, 63);

        VarroXMLReader reader = new VarroXMLReader();

        reader.parse(new int[] {867, 1140}, "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesFuture.xml",
                "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesNANFuture.xml");

        reader.process(0.01, true);

        DataTable sentenceFeatures = new DataTable();
        sentenceFeatures.content = reader.sentenceVector;
        sentenceFeatures.calcColumnSize();

        System.out.println("original sentence feature length: " + sentenceFeatures.content.size());

        System.out.println("original file length: " + originalF.content.size());

        DataTable newTable = replaceID(originalF, sentenceFeatures);

//        newTable.content.entrySet().stream().forEach(e -> System.out.println(e.getKey()));
        DataFile train = DataFile.create(FileType.TabFile, 0, 1, false).readIn("E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\nfold_2_train.txt");
        DataFile test = DataFile.create(FileType.TabFile, 0, 1, false).readIn("E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\nfold_2_test.txt");

        DataFile sum = train.append(test);

        System.out.println("combined training testing size: " + sum.content.size());

        DataTable transformed = sum.toDataTable();

        System.out.println("combined training testing size after table: " + transformed.content.size());

        System.out.println("New table size: " + newTable.content.size());

        DataTable finalTable = DataTable.merge(newTable, transformed);

        // finalTable.content.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue().get(finalTable.targetColumn)));

        System.out.println(finalTable.targetColumn);
        System.out.println(finalTable.content.size());

        //2nd iteration (where things go wrong)

        DataFile train2 = DataFile.create(FileType.TabFile, 0, 1, false).readIn("E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\nfold_3_train.txt");
        DataFile test2 = DataFile.create(FileType.TabFile, 0, 1, false).readIn("E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\nfold_3_test.txt");

        DataFile sum2 = train2.append(test2);

        DataTable transformed2 = sum2.toDataTable();

        DataTable nextFinalTable = DataTable.merge(newTable, transformed2);

        System.out.println(nextFinalTable.targetColumn);
//        nextFinalTable.content.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue().get(finalTable.targetColumn)));
        System.out.println(nextFinalTable.content.entrySet().size());

        long et = System.currentTimeMillis();
        System.out.println("time used: " + (et - st));
    }

    private DataTable replaceID(DataFile originalF, DataTable sentenceFeature) {
        int counter = 0;
        //sentenceID -> sequence Number
        Map<Integer, String> idMap = new HashMap<Integer, String>();
        for (ArrayList<String> row : originalF.content) {
            idMap.put(counter++, row.get(originalF.idColumn));
        }

        counter = 0;
        Map<String, ArrayList<String>> newContentWithRightKey = new HashMap<String, ArrayList<String>>();
        //then map with our sentenceFeatures
        for (Map.Entry<String, ArrayList<String>> en : sentenceFeature.content.entrySet()) {
            Integer seqNum = Integer.parseInt(en.getKey().split("_")[3]);

            newContentWithRightKey.put(idMap.get(seqNum), en.getValue());
        }

        System.out.println("number of duplication: " + counter);

        sentenceFeature.content = newContentWithRightKey; //replace old one

        return sentenceFeature;
    }

    public void startClassification(DataTable sentenceFeatures) throws Exception {
        SubtreeTrainer classifier = new SubtreeTrainer(1, 5);

        File savingTrain = new File("E:\\Allen\\R\\naacl2015\\nFoldMalletSubtree\\MTurkAllSentences_Processed_malletTrainSet100.txt");
        String nFoldSavingDir = "E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\";
        String featureFile = "E:\\Allen\\R\\naacl2015\\MTurkAllSentences_Processed.csv";

        addClassifiers(classifier);

        //last one is whether data reading in is SVM format (1:5 5:7)
        classifier.train(savingTrain, nFoldSavingDir, featureFile, sentenceFeatures, 1, 67, true, new int[] {0, 2, 3, 4, 66, 65, 64, 63}, false);
    }

    public void addClassifiers(SubtreeTrainer classifier) throws Exception {

        String loggerDir = "E:\\Allen\\R\\naacl2015\\nFoldMalletSubtree\\";
        String savingDir = "E:\\Allen\\R\\naacl2015\\nFoldMalletSubtree\\";
        String savingTest = "E:\\Allen\\R\\naacl2015\\nFoldMalletSubtree\\";


        classifier.addClassifierTrainers(new MaxEntTrainer());
        classifier.addLoggerFiles(loggerDir + "maxEntTiralLogger.txt"); //method inside will take care of trailing slash
        classifier.addSavingDir(savingDir+"maxEntClassifier.ser");
        classifier.addSavingTest(savingTest+"maxEntTest.txt");

        classifier.addClassifierTrainers(new NaiveBayesTrainer()); //same as Mallet's default choice
        classifier.addLoggerFiles(loggerDir + "naiveBayesTiralLogger.txt");
        classifier.addSavingDir(savingDir+"naiveBayesClassifier.ser");
        classifier.addSavingTest(savingTest+"naiveBayesTest.txt");

        String[] kernelMethods = "1:3".split(":");

        for (String method: kernelMethods) {
            classifier.addClassifierTrainers(new SVMTrainer(method)); //this will throw fatal error if method doesn't match
            classifier.addLoggerFiles(loggerDir + "SVMTiralLogger"+method+".txt");
            classifier.addSavingDir(savingDir+"SVMClassifier"+method+".ser"); //emmmm, may not really work on loading, saving should be fine
            classifier.addSavingTest(savingTest+"svmTest"+method+".txt");
        }
    }

    @Test
    public void ldaGenerate() throws Exception {

        long st = System.currentTimeMillis();

        VarroXMLReader reader = new VarroXMLReader();

        reader.parse(new int[] {867}, "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesFuture.xml");

        reader.generateLDAFile(0.15);
        System.out.println(reader.ldaSentences.size());

        System.out.println("saving phase starts!");

        reader.printLDAFile("E:\\Allen\\R\\naacl2015\\subtree\\sentences");

        long et = System.currentTimeMillis();

        System.out.println("done!");
        System.out.println("time used: " + (et - st));
    }

    @Test
    public void testMap() throws Exception {
        Map<String, Double> subtreeList = new HashMap<String, Double>();
        subtreeList.put("thisTree", -5.0);
        double newValue = 4.0;
        subtreeList.computeIfPresent("thatTree", (string, val) -> val + newValue);
        subtreeList.putIfAbsent("thatTree", newValue);
        System.out.println(subtreeList.get("thisTree"));
        System.out.println(subtreeList.get("thatTree"));
    }


}