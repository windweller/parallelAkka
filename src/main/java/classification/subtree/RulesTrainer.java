package classification.subtree;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import classification.GenericTrainer;
import classification.Trial;
import classification.file.DataFile;
import classification.file.DataTable;
import classification.file.FileType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by anie on 3/5/2015.
 *
 * SubtreeTrainer is a special kind of trainer
 * It doesn't split instances
 * It reads in a directory where split up testing/training files
 * are stored
 */
public class RulesTrainer {

    public int idCol;
    public int targetCol;
    public boolean header;
    public int[] dropCols;
    public String featureFile;

    double trainPercentage;
    int crossValidation;
    ArrayList<Path> loggerFiles = new ArrayList<>(); //for cross-validation only
    ArrayList<ClassifierTrainer> classifierTrainers = new ArrayList<ClassifierTrainer>();
    ArrayList<File> savingDir = new ArrayList<File>();
    ArrayList<File> savingTest = new ArrayList<File>();

    public void addSavingTest(String savingLoc) {
        savingTest.add(new File(savingLoc));
    }

    public void addSavingDir(String savingLoc) {
        savingDir.add(new File(savingLoc));
    }

    public void addClassifierTrainers(ClassifierTrainer classifierTrainer) {
        this.classifierTrainers.add(classifierTrainer);
    }

    public boolean isClassifierEmpty() {
        return classifierTrainers.size() == 0;
    }

    public void addLoggerFiles(String loggerFile) {
        this.loggerFiles.add(Paths.get(loggerFile));
    }

    public RulesTrainer(double trainPercentage, int crossValidation) {
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
    }

    public RulesTrainer(double trainPercentage, int crossValidation, String[] loggerFiles, ClassifierTrainer... classifiers) {
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
        for (String loggerFile: loggerFiles) this.loggerFiles.add(Paths.get(loggerFile));

        Collections.addAll(this.classifierTrainers, classifiers);
    }

    /**
     * this assumes Tinker standard
     * nFold standard
     * This also preprocesses and combine files
     * to feature file
     */
    public DataTable loadNFoldFile(String nfoldSavingDir, int nth) throws IOException {

        String trainFile = nfoldSavingDir + "nfold_"+ nth + "_train.txt";
        String testFile = nfoldSavingDir + "nfold_"+ nth + "_test.txt";

        DataFile train = DataFile.create(FileType.TabFile, 0, 1, false).readIn(trainFile);
        DataFile test = DataFile.create(FileType.TabFile, 0, 1, false).readIn(testFile);

        DataFile sum = train.append(test);

        DataFile features = DataFile.create(FileType.CSVFile, idCol, targetCol, header).readIn(featureFile);

        if (dropCols != null)
            features.dropCols(dropCols);

        return DataFile.merge(sum, features); //feature's targetCol will be dropped
    }

    public Classifier[] subtreeTrain(File savingTrain, ArrayList<File> savingTest,
                                     String nfoldSavingDir, boolean SVMFormat) throws IOException {

        Classifier[] classifiers = new Classifier[classifierTrainers.size()];
        Trial[] trialRunners = new Trial[classifierTrainers.size()];

        SubtreePipeBuilder spb = null;

        for (int i = 0; i < classifierTrainers.size(); i++) {
            trialRunners[i] = new Trial(classifierTrainers.get(i)); //you need individual ct for every ct
        }

        //we start the iteration
        if (crossValidation > 1) {

            double splitPercentage = 1/(double) crossValidation;

            for (int i = 1; i <= crossValidation; i++) {

                DataTable dt = loadNFoldFile(nfoldSavingDir, i);

                //we initialize spb for only once!
                if (spb == null) {spb = new SubtreePipeBuilder(idCol, dt.targetColumn, header, SVMFormat);}

                InstanceList instances = spb.readingFromDataTable(dt);

                InstanceList[] instanceList = instances.splitInOrder(new double[] {1-splitPercentage, splitPercentage}); //so it's the same split as before!
                InstanceList trainingData = instanceList[0];
                InstanceList testingData = instanceList[1];

                for (int j = 0; j < trialRunners.length; j++) {
                    Classifier classifier = trialRunners[j].run(trainingData,testingData, loggerFiles.get(j), i); //a single run, same split each fold for every trainer
                    GenericTrainer.saveNFoldTestResult(testingData, classifier, savingTest.get(j));
                }
            }

            for (int i = 0; i < trialRunners.length; i++) {
                classifiers[i] = trialRunners[i].bestClassifier;
                GenericTrainer.appendToLogger(loggerFiles.get(i),
                        "The "+ crossValidation +"-fold total testing accuracy is: " + trialRunners[i].getAvgAccuracy() + "\r\n");
            }
        }

        return classifiers;
    }

    //you need to save best classifiers (
    public void train(File savingTrain, String nfoldSavingDir,
                      String featureFile, int idCol, int targetCol, boolean header,
                      int[] dropCols, boolean SVMFormat) throws IOException {
        this.idCol = idCol;
        this.targetCol = targetCol;
        this.header = header;
        this.featureFile = featureFile;
        this.dropCols = dropCols;

        Classifier[] classifiers = null;

        if (trainPercentage == (double) 1) {

            //savingTest would now save the cross-validation result
            classifiers = subtreeTrain(savingTrain, savingTest, nfoldSavingDir, SVMFormat);
        }

        GenericTrainer.save(classifiers, savingDir);

    }


}
