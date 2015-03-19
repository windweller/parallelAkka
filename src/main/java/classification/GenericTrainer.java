package classification;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import classification.file.DataFile;
import classification.file.DataTable;
import classification.file.FileType;
import classification.subtree.DataTable2FeatureVectorAndLabel;
import classification.subtree.DataTableIterator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * This trainer is invoked if we are running
 * more than one algorithm
 */
public class GenericTrainer {

    Pipe pipe;
    public int[] gramFlag;
    ArrayList<PipeBuilder> pipeBuilders = new ArrayList<>();
    double trainPercentage;
    int crossValidation;
    ArrayList<Path> loggerFiles = new ArrayList<>(); //for cross-validation only
    ArrayList<ClassifierTrainer> classifierTrainers = new ArrayList<ClassifierTrainer>();
    ArrayList<File> savingDir = new ArrayList<File>();
    ArrayList<File> savingTest = new ArrayList<File>();

    public String nfoldSavingDir;

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

    public void addPipeBuilder(PipeBuilder pipeBuilder) {
        this.pipeBuilders.add(pipeBuilder);
    }

    public GenericTrainer(double trainPercentage, int crossValidation) {
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
    }

    public GenericTrainer(double trainPercentage, int crossValidation, String[] loggerFiles, ClassifierTrainer... classifiers) {
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
        for (String loggerFile: loggerFiles) this.loggerFiles.add(Paths.get(loggerFile));

        Collections.addAll(this.classifierTrainers, classifiers);
    }

    //Evaluate has saving to file power too
    public void evaluate(InstanceList instancesTesting,
                                Classifier classifier, File savingTest) throws FileNotFoundException, UnsupportedEncodingException {

        String label1 = classifier.getLabelAlphabet().lookupLabel(0).toString();
        String label2 = classifier.getLabelAlphabet().lookupLabel(1).toString();

        System.out.println("The testing accuracy is: " + classifier.getAccuracy(instancesTesting));
        System.out.println("F1 for class "+label1+" is: " +  classifier.getF1(instancesTesting, 0));
        System.out.println("F1 for class"+label2+" is: " + classifier.getF1(instancesTesting, 1));

        if (savingTest != null) {
            PrintStream out2 = new PrintStream(savingTest, "UTF-8");
            for (Instance instance: instancesTesting) {
                Labeling labeling = classifier.classify(instance).getLabeling();

                StringBuilder output2 = new StringBuilder();
                output2.append(instance.getName());

                for (int location = 0; location < labeling.numLocations(); location++) {
                    output2.append("\t").append(labeling.labelAtLocation(location));
                    output2.append("\t").append(labeling.valueAtLocation(location));
                }
                out2.println(output2);
            }
            out2.flush();
            out2.close();
        }
    }

    /**
     * Create files inside this directory
     * Special way, it creates two files: a training file and a testing file
     * @param savingDir should be in form of ".../somedir/", we will manually add trailing "/"
     * @param fileName should be something like "nfold_1", and we will turn them into "nfold1_train.txt" and "nfold_1_test.txt"
     * @param trainingData
     * @param testingData
     */
    public void saveNFold(String savingDir, String fileName, InstanceList trainingData, InstanceList testingData)
            throws FileNotFoundException, UnsupportedEncodingException {

        if (!savingDir.endsWith("\\")) savingDir += "\\";

        this.saveInstance(new File(savingDir+fileName+"_train.txt"),trainingData);
        this.saveInstance(new File(savingDir+fileName+"_test.txt"),testingData);

    }

    /**
     * This keeps APPEND test result to the same folder
     * @param instancesTesting
     * @param classifier
     * @param savingTest
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void saveNFoldTestResult(InstanceList instancesTesting,
                                    Classifier classifier, File savingTest) throws FileNotFoundException, UnsupportedEncodingException {

        PrintStream out = new PrintStream(new FileOutputStream(savingTest, true));

        String label1 = classifier.getLabelAlphabet().lookupLabel(0).toString();
        String label2 = classifier.getLabelAlphabet().lookupLabel(1).toString();

        for (Instance instance: instancesTesting) {
            Labeling labeling = classifier.classify(instance).getLabeling();

            StringBuilder output2 = new StringBuilder();
            output2.append(instance.getName());

            for (int location = 0; location < labeling.numLocations(); location++) {
                output2.append("\t").append(labeling.labelAtLocation(location));
                output2.append("\t").append(labeling.valueAtLocation(location));
            }
            out.println(output2);
        }
        out.flush();
        out.close();
    }

    public static void appendToLogger(Path logger, String content) throws IOException {
        Files.write(logger, "\r\n".getBytes(), StandardOpenOption.APPEND);
        Files.write(logger, content.getBytes(), StandardOpenOption.APPEND);
    }

    public void saveInstance(File savingTrain, InstanceList instancesTraining) throws FileNotFoundException, UnsupportedEncodingException {

        PrintStream out = new PrintStream(savingTrain, "UTF-8");
        for (Instance instance : instancesTraining) {
            StringBuilder output = new StringBuilder();
            output.append(instance.getName()).append("\t");
            output.append(instance.getTarget());
            out.println(output);
        }
        out.flush();
        out.close();
    }


    //Now this has N-fold validation implementation
    public Classifier[] GenericTrain(InstanceList instancesTraining,
                                  InstanceList instancesTesting,
                                  File savingTrain,
                                  ArrayList<File> savingTest, String nfoldSavingDir) throws IOException {

        Alphabet dictionary = instancesTraining.getAlphabet();
        Classifier[] classifiers = new Classifier[classifierTrainers.size()];
        Trial[] trialRunners = new Trial[classifierTrainers.size()];


        for (int i = 0; i < classifierTrainers.size(); i++) {
            trialRunners[i] = new Trial(classifierTrainers.get(i)); //you need individual ct for every ct
        }

        if (crossValidation > 1) {

            //split up once, then reuse them
            double splitPercentage = 1/(double) crossValidation;

            for (int i = 1; i <= crossValidation; i++) {

                InstanceList[] instanceList = instancesTraining.split(new double[] {1-splitPercentage, splitPercentage});
                InstanceList trainingData = instanceList[0];
                InstanceList testingData = instanceList[1];

                if (nfoldSavingDir != null)
                this.saveNFold(nfoldSavingDir, "nfold_"+i, trainingData, testingData);

                for (int j = 0; j < trialRunners.length; j++) {
                    Classifier classifier = trialRunners[j].run(trainingData,testingData, loggerFiles.get(j), i); //a single run, same split each fold for every trainer
                    this.saveNFoldTestResult(testingData, classifier, savingTest.get(j));
                }
            }

            //when there is a split for testing/training
            if (instancesTesting != null) {
                //after trials, get the best classifier out and evaluate with testing data
                for (int i = 0; i < classifierTrainers.size(); i++) {
                    classifiers[i] = trialRunners[i].bestClassifier;
                    this.evaluate(instancesTesting, classifiers[i], savingTest.get(i));
                }
            }
            else {
                //if training is 100%, we get average accuracy out
                for (int i = 0; i < trialRunners.length; i++) {
                    classifiers[i] = trialRunners[i].bestClassifier;
                    this.appendToLogger(loggerFiles.get(i),
                            "The "+ crossValidation +"-fold total testing accuracy is: " + trialRunners[i].getAvgAccuracy() + "\r\n");
                }
            }
        }
        else{
            System.out.println("Cross Validation smaller than 1 is silently ignored.");

            for (int i = 1; i <= classifierTrainers.size(); i++) {

                Classifier classifier = classifierTrainers.get(i).train(instancesTraining);
                classifiers[i] = classifier;

                String label1 = classifier.getLabelAlphabet().lookupLabel(0).toString();
                String label2 = classifier.getLabelAlphabet().lookupLabel(1).toString();

                System.out.println("The testing accuracy is: " + classifier.getAccuracy(instancesTesting));
                System.out.println("F1 for class "+label1+" is: " +  classifier.getF1(instancesTraining, 0));
                System.out.println("Accuracy for class"+label2+" is: " + classifier.getF1(instancesTraining, 1));
            }

            for (int i = 1; i <= classifiers.length; i++) {
                this.evaluate(instancesTesting, classifiers[i], savingTest.get(i));
            }
        }

        if (savingTrain != null) {
            this.saveInstance(savingTrain, instancesTraining);
        }

        return classifiers;
    }

    /**
     *
     * @param directory this should be a single FILE! Not a directory
     * @param savingTrain
     * @param nfoldSavingDir this should be a directory to save nfold split data
     * @throws IOException
     */
    public void train(String directory, File savingTrain, String nfoldSavingDir,
                      String featureFile, int idCol, int targetCol, boolean header,
                      int[] dropCols) throws IOException {

        InstanceList instances = pipeBuilders.get(0).readingSingleFile(directory);

        if (!nfoldSavingDir.endsWith("\\")) nfoldSavingDir += "\\";
        this.nfoldSavingDir = nfoldSavingDir;

        Classifier[] classifiers = null;

        if (trainPercentage == (double) 1) {

            //savingTest would now save the cross-validation result
            classifiers = this.GenericTrain(instances, null, savingTrain, savingTest, nfoldSavingDir);
        }
        else {
            InstanceList[] instanceList = instances.split(new double[] {trainPercentage, 1-trainPercentage});
            InstanceList trainingData = instanceList[0];
            InstanceList testingData = instanceList[1];

            classifiers = this.GenericTrain(trainingData, testingData, savingTrain, savingTest, nfoldSavingDir);
        }

        GenericTrainer.save(classifiers, savingDir);


        if (pipeBuilders.size() > 1) {
            //enter phase 2, we need to read in all the saved training instances, build data table
            combineFiles(featureFile, idCol, targetCol, header, dropCols);
        }

    }

    /* Phase 2 related helper functions */

    //combine training file with testing file, so they
    //pass down same pipeline, can be splitInOrder()
    public void combineFiles(String featureFile, int idCol, int targetCol,
                             boolean header, int[] dropCols) throws IOException {

        for (int i = 1; i <= crossValidation; i++){
            String trainFile = nfoldSavingDir + "nfold_"+ i + "_train.txt";
            String testFile = nfoldSavingDir + "nfold_"+ i + "_test.txt";

            DataFile train = DataFile.create(FileType.TabFile, 0, 1, false).readIn(trainFile);
            DataFile test = DataFile.create(FileType.TabFile, 0, 1, false).readIn(testFile);

            DataFile sum = train.append(test);

            DataFile features = DataFile.create(FileType.CSVFile, idCol, targetCol, header).readIn(featureFile);

            if (dropCols != null)
                features.dropCols(dropCols);

            DataTable sumWithFeatures = DataFile.merge(sum, features); //feature's targetCol will be dropped

        }
    }


    /* Utility functions */

    public static void save(Classifier[] classifiers, ArrayList<File> serializedFiles) throws IOException {
        for (int i = 0; i < classifiers.length; i++) {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream((serializedFiles.get(i))));
            oos.writeObject(classifiers[i]);
            oos.close();
        }
    }

    public static Classifier load(File serializedFile) throws IOException, ClassNotFoundException {
        Classifier classifier;

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }


}