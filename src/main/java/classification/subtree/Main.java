package classification.subtree;

import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import classification.file.DataTable;
import classification.subtree.varro.VarroXMLReader;
import classification.svm.SVMTrainer;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * This is a new CLI because the demand is very different
 * from normal NGram classification
 */
public class Main {

    public static void main(String [ ] args) throws Exception {
//        Parser parser = new GnuParser();
//        Options options = new Options();
//
//        //This will search the files for "test" and "train" marker, with a int 1,2,3..
//        options.addOption("d", "fileDir", false, "specify the directory contains training and testing files");
//        options.addOption("n", "nFold", true, "specify the max number of nFold, default 5");
//        options.addOption("p", "parentFile", true, "specify a parent file with all features");
//
//        CommandLine cmd = null;
//        try {cmd = parser.parse(options, args);} catch(ParseException e) {usage(options);}

        VarroXMLReader reader = new VarroXMLReader();

        reader.parse(new int[] {867, 1140}, "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesFuture.xml",
                "E:\\Allen\\R\\naacl2015\\subtree\\mTurkAllSentencesNANFuture.xml");

        reader.process(0.005, false);
        DataTable sentenceFeatures = new DataTable();
        sentenceFeatures.content = reader.sentenceVector;
        sentenceFeatures.calcColumnSize(); //this is crucial!!!!!!!!

        startClassification(sentenceFeatures);
    }

    public static void startClassification(DataTable sentenceFeatures) throws Exception {
        SubtreeTrainer classifier = new SubtreeTrainer(1, 5);

        File savingTrain = new File("E:\\Allen\\R\\naacl2015\\nFoldMalletSubtree\\MTurkAllSentences_Processed_malletTrainSet100.txt");
        String nFoldSavingDir = "E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\";
        String featureFile = "E:\\Allen\\R\\naacl2015\\MTurkAllSentences_Processed.csv";

        addClassifiers(classifier);

        //last one is whether data reading in is SVM format (1:5 5:7)
        classifier.train(savingTrain, nFoldSavingDir, featureFile, sentenceFeatures, 1, 67, true, new int[] {0, 2, 3, 4, 66, 65, 64, 63}, false);
    }

    public static void addClassifiers(SubtreeTrainer classifier) throws Exception {

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

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Mallet Classifier", options);
    }
}
