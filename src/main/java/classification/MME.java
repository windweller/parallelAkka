package classification;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created by anie on 1/27/2015.
 */
public class MME {
    Pipe pipe;
    int[] gramFlag;
    double trainPercentage;
    int crossValidation;
    Path loggerFile; //for cross-validation only

    public MME(int[] gramFlag, double trainPercentage, int crossValidation, String loggerFile) {
        pipe = buildPipe();
        this.gramFlag = gramFlag;
        this.trainPercentage = trainPercentage;
        this.crossValidation = crossValidation;
        this.loggerFile = Paths.get(loggerFile);
    }

    private Pipe buildPipe() {
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+"); //punctuations are removed

        pipeList.add(new Input2CharSequence ());

        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        pipeList.add(new TokenSequenceLowercase());

        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        pipeList.add(new TokenSequenceNGrams(new int[] {3,2,1}));

        pipeList.add(new TokenSequence2FeatureSequence()); //this as well...

        pipeList.add(new Target2Label());

        pipeList.add(new FeatureSequence2FeatureVector());

        pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }

    public InstanceList readingSingleFile(File directory)
            throws FileNotFoundException, UnsupportedEncodingException {

        InstanceList instances = new InstanceList(pipe);
        Reader fileReader = new InputStreamReader(new FileInputStream(directory), "UTF-8");
        instances.addThruPipe(new CsvIterator(fileReader,
                Pattern.compile("(\\w+?),(\\w+?),(.+)"), 3, 2, 1));

        return instances;
    }

    //Now this has N-fold validation implementation
    public Classifier MaxEntTrain(InstanceList instancesTraining,
                                  InstanceList instancesTesting,
                                  File savingTrain,
                                  File savingTest) throws IOException {

        Alphabet dictionary = instancesTraining.getAlphabet();
        Classifier classifier = null;

        if (crossValidation > 1) {
            Trial trialRunner = new Trial(new MaxEntTrainer(), crossValidation);
            classifier = trialRunner.run(instancesTraining, loggerFile);

            String label1 = classifier.getLabelAlphabet().lookupLabel(1).toString();
            String label2 = classifier.getLabelAlphabet().lookupLabel(2).toString();

            System.out.println("The testing accuracy is: " + classifier.getAccuracy(instancesTesting));
            System.out.println("F1 for class "+label1+" is: " +  classifier.getF1(instancesTesting, 1));
            System.out.println("Accuracy for class"+label2+" is: " + classifier.getF1(instancesTesting, 2));

        }
        else{
            System.out.println("Cross Validation smaller than 1 is silently ignored.");

            ClassifierTrainer trainer = new MaxEntTrainer();
            classifier = trainer.train(instancesTraining);

            String label1 = classifier.getLabelAlphabet().lookupLabel(1).toString();
            String label2 = classifier.getLabelAlphabet().lookupLabel(2).toString();

            System.out.println("The testing accuracy is: " + classifier.getAccuracy(instancesTesting));
            System.out.println("F1 for class "+label1+" is: " +  classifier.getF1(instancesTesting, 1));
            System.out.println("Accuracy for class"+label2+" is: " + classifier.getF1(instancesTesting, 2));
        }

        if (savingTrain != null) {
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

        return classifier;
    }

    public Classifier MaxEntTrain(InstanceList instancesTraining) throws IOException {
        Alphabet dictionary = instancesTraining.getAlphabet();
        Classifier classifier = null;

        if (crossValidation > 1) {
            Trial trialRunner = new Trial(new MaxEntTrainer(), crossValidation);
            classifier = trialRunner.run(instancesTraining, loggerFile);

            String label1 = classifier.getLabelAlphabet().lookupLabel(1).toString();
            String label2 = classifier.getLabelAlphabet().lookupLabel(2).toString();

            System.out.println("The testing accuracy is: " + classifier.getAccuracy(instancesTraining));
            System.out.println("F1 for class "+label1+" is: " +  classifier.getF1(instancesTraining, 1));
            System.out.println("Accuracy for class"+label2+" is: " + classifier.getF1(instancesTraining, 2));
        }
        else {
            ClassifierTrainer trainer = new MaxEntTrainer();
            classifier = trainer.train(instancesTraining);

            String label1 = classifier.getLabelAlphabet().lookupLabel(1).toString();
            String label2 = classifier.getLabelAlphabet().lookupLabel(2).toString();

            System.out.println("The training accuracy is: " + classifier.getAccuracy(instancesTraining));
            System.out.println("F1 for class "+label1+" is: " +  classifier.getF1(instancesTraining, 1));
            System.out.println("Accuracy for class"+label2+" is: " + classifier.getF1(instancesTraining, 2));
        }

        return classifier;
    }

    public void train(File directory, File savingDir, File savingTrain, File savingTest) throws IOException {
        InstanceList instances = this.readingSingleFile(directory);

        Classifier classifier;

        if (trainPercentage == 1) {
            classifier = this.MaxEntTrain(instances);
        }
        else {
            InstanceList[] instanceList = instances.split(new double[] {trainPercentage, 1-trainPercentage});
            InstanceList trainingData = instanceList[0];
            InstanceList testingData = instanceList[1];

            classifier = this.MaxEntTrain(trainingData, testingData, savingTrain, savingTest);
        }

        MME.save(classifier, savingDir);
    }

    /**
     *
     * The input should satisfy [name] [data]
     * It will be extracted through pattern: (\w+),(.+)
     * target group (label group) is fed in with wrong data
     * but is silently ignored.
     *
     * This method is modified after cc.mallet.classify.tui.Csv2Classify
     * to fix Alphabets doesn't match problem
     *
     * @param classifier
     * @param directory
     * @param outputLoc
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void classify(Classifier classifier, File directory, File outputLoc)
            throws FileNotFoundException, UnsupportedEncodingException {
        InstanceList instances = new InstanceList(classifier.getInstancePipe());

        Reader fileReader = new InputStreamReader(new FileInputStream(directory), "UTF-8");

        Iterator<Instance> csvIterator =  new CsvIterator(fileReader,
                Pattern.compile("(.+?),(.+)"), 2, 0, 1);

        Iterator<Instance> iterator =
                classifier.getInstancePipe().newIteratorFrom(csvIterator);

        PrintStream out = new PrintStream(outputLoc, "UTF-8");

        // Stop growth on the alphabets. If this is not done and new
        // features are added, the feature and classifier parameter
        // indices will not match.
        classifier.getInstancePipe().getDataAlphabet().stopGrowth();
        classifier.getInstancePipe().getTargetAlphabet().stopGrowth();

        while (iterator.hasNext()) {
            Instance instance = iterator.next();

            Labeling labeling =
                    classifier.classify(instance).getLabeling();

            StringBuilder output = new StringBuilder();
            output.append(instance.getName());

            for (int location = 0; location < labeling.numLocations(); location++) {
                output.append("\t").append(labeling.labelAtLocation(location));
                output.append("\t").append(labeling.valueAtLocation(location));
            }
            out.println(output);
        }

//        instances.addThruPipe(new CsvIterator(fileReader,
//                Pattern.compile("(.+?),(.+)"), 2, 0, 1));

//        ArrayList<Classification> classifications = classifier.classify(instances);
//
//        PrintWriter pw = new PrintWriter(output);
//
//        for (Classification classification: classifications) {
//            classification.print(pw);
//        }
//
        System.out.println("All finished");
    }

    public static void save(Classifier classifier, File serializedFile) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream((serializedFile)));
        oos.writeObject(classifier);
        oos.close();
    }

    public static Classifier load(File serializedFile) throws IOException, ClassNotFoundException {
        Classifier classifier;

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }
}
