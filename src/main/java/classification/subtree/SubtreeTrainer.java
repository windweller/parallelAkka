package classification.subtree;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.types.InstanceList;
import classification.GenericTrainer;
import classification.Trial;
import classification.file.DataFile;
import classification.file.DataTable;
import classification.file.FileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anie on 3/6/2015.
 */
public class SubtreeTrainer {

    public int idCol;
    public int targetCol;
    public boolean header;
    public int[] dropCols;
    public String originalFile;
    public DataTable sentenceFeature;

    boolean initialRun = true;

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

    public SubtreeTrainer(double trainPercentage, int crossValidation) {
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
    }

    public SubtreeTrainer(double trainPercentage, int crossValidation, String[] loggerFiles, ClassifierTrainer... classifiers) {
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
        for (String loggerFile: loggerFiles) this.loggerFiles.add(Paths.get(loggerFile));

        Collections.addAll(this.classifierTrainers, classifiers);
    }

    public DataTable loadNFoldFile(String nfoldSavingDir, int nth) throws IOException {

        String trainFile = nfoldSavingDir + "nfold_"+ nth + "_train.txt";
        String testFile = nfoldSavingDir + "nfold_"+ nth + "_test.txt";

        DataFile train = DataFile.create(FileType.TabFile, 0, 1, false).readIn(trainFile);
        DataFile test = DataFile.create(FileType.TabFile, 0, 1, false).readIn(testFile);

        DataFile sum = train.append(test);



        if (initialRun) {
            DataFile originalF = DataFile.create(FileType.CSVFile, idCol, targetCol, header).readIn(originalFile);
            replaceID(originalF);
        }


        DataTable temp = sum.toDataTable();

        DataTable result = DataTable.merge(temp, sentenceFeature);

        sentenceFeature.targetColumn = result.targetColumn;

        //since there's always this mysterious last column problem, we always drop after initial run
        if (!initialRun) {
            result.content.entrySet().stream().forEach(e -> e.getValue().remove(e.getValue().size()));
        }

        return result; //feature's targetCol will be dropped
    }

    public void replaceID(DataFile originalF) {
        int counter = 0;
        //sentenceID -> sequence Number
        Map<Integer, String> idMap = new HashMap<Integer, String>();
        for (ArrayList<String> row : originalF.content) {
            idMap.put(counter++, row.get(originalF.idColumn));
        }

        Map<String, ArrayList<String>> newContentWithRightKey = new HashMap<String, ArrayList<String>>();
        //then map with our sentenceFeatures
        for (Map.Entry<String, ArrayList<String>> en : sentenceFeature.content.entrySet()) {
            Integer seqNum = 0;
            try {seqNum = Integer.parseInt(en.getKey().split("_")[3]);
            }catch (Exception e) {
                for (String s :  en.getKey().split("_")) {
                    System.out.println(s + " ");
                }
                System.exit(0);
            }

            newContentWithRightKey.put(idMap.get(seqNum), en.getValue());
        }

        sentenceFeature.content = newContentWithRightKey; //replace old one
        initialRun = false;
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
                      String originalFile, DataTable sentenceFeature, int idCol, int targetCol, boolean header,
                      int[] dropCols, boolean SVMFormat) throws IOException {
        this.idCol = idCol;
        this.targetCol = targetCol;
        this.header = header;
        this.originalFile = originalFile;
        this.sentenceFeature = sentenceFeature;
        this.dropCols = dropCols;

        Classifier[] classifiers = null;

        if (trainPercentage == (double) 1) {

            //savingTest would now save the cross-validation result
            classifiers = subtreeTrain(savingTrain, savingTest, nfoldSavingDir, SVMFormat);
        }

        GenericTrainer.save(classifiers, savingDir);

    }
}
