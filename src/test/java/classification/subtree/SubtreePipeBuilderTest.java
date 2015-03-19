package classification.subtree;

import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import classification.svm.SVMTrainer;
import org.junit.Test;

import java.io.File;

public class SubtreePipeBuilderTest {

    @Test
    public void testReadingSingleFile() throws Exception {
        RulesTrainer classifier = new RulesTrainer(1, 5);

        File savingTrain = new File("E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\MTurkAllSentences_Processed_malletTrainSet100.txt");
        String nFoldSavingDir = "E:\\Allen\\R\\naacl2015\\nFoldMalletNgram\\nFoleTrainTest\\";
        String featureFile = "E:\\Allen\\R\\naacl2015\\MTurkAllSentences_Processed.csv";

        addClassifiers(classifier);

        //last one is whether data reading in is SVM format (1:5 5:7)
        classifier.train(savingTrain, nFoldSavingDir, featureFile, 1, 67, true, new int[] {0, 2, 3, 4, 66, 65, 64, 63}, false);
    }

    public void addClassifiers(RulesTrainer classifier) throws Exception {

        String loggerDir = "E:\\Allen\\R\\naacl2015\\nFoldRulesMallet\\";
        String savingDir = "E:\\Allen\\R\\naacl2015\\nFoldRulesMallet\\";
        String savingTest = "E:\\Allen\\R\\naacl2015\\nFoldRulesMallet\\";


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
}