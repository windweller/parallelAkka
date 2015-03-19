package classification;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.types.InstanceList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

/**
 * This class is used for cross-validation
 */
public class Trial {

    ClassifierTrainer trainer;
    ArrayList<Double> accuracyEachTrial;
    double bestAccuracy = 0;
    public Classifier bestClassifier;
    int numOfTrials;
    double splitPercentage;

    /**
     * This constructor is used in conjunction with singleRun()
     */
    public Trial(ClassifierTrainer trainer) {
        this.trainer = trainer;
        this.numOfTrials = 0;
        accuracyEachTrial = new ArrayList<>();
    }

    public Trial(ClassifierTrainer trainer, int numOfTrials) {
        this.trainer = trainer;
        this.numOfTrials = numOfTrials;
        this.splitPercentage = 1/(double) numOfTrials;
        accuracyEachTrial = new ArrayList<>();
    }

    /**
     * This is the activation function
     * @param instances there's no training/test difference here, all instances passed in should be training data
     * @param logFile a path for accuracy tracking
     * @throws IOException
     */
    public Classifier run(InstanceList instances, Path logFile) throws IOException {

        for (int i = 1; i <= numOfTrials; i++) {
            //since splitPercentage will be small, like 10%, first is for training, second is for testing

            InstanceList[] instanceList = instances.split(new double[] {1-splitPercentage, splitPercentage});
            InstanceList trainingData = instanceList[0];
            InstanceList testingData = instanceList[1];

            Classifier classifier = this.train(trainingData);

            String label1 = classifier.getLabelAlphabet().lookupLabel(0).toString();
            String label2 = classifier.getLabelAlphabet().lookupLabel(1).toString();
            Double currentAccuracy =  classifier.getAccuracy(testingData);

            Files.write(logFile, ("The "+ i +"th testing accuracy is: " + currentAccuracy + "\r\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            Files.write(logFile, ("The "+ i +"th testing F1 for "+label1+" is: " + classifier.getF1(testingData, 0)+ "\r\n").getBytes(), StandardOpenOption.APPEND);
            Files.write(logFile, ("The "+ i +"th testing F1 for "+label2+" is: " + classifier.getF1(testingData, 1)+ "\r\n").getBytes(), StandardOpenOption.APPEND);
            Files.write(logFile, "\r\n".getBytes(), StandardOpenOption.APPEND);

            if (currentAccuracy > bestAccuracy) {
                bestAccuracy = currentAccuracy;
                bestClassifier = classifier;
            }

            accuracyEachTrial.add(currentAccuracy);
        }

        return bestClassifier; //something went wrong, we return null
    }

    /**
     * This is made for GenericTrainer to call; the splitting data are fed from outside
     * to remain consistency. Will not return anything, so user has to access the bestClassifier
     * and bestAccuracy to get result
     * @param i This is the ordinal trial number
     * @param trainingData
     * @param testingData
     * @param logFile
     * @return classifier so you can run test on it
     * @throws IOException
     */
    public Classifier run(InstanceList trainingData, InstanceList testingData, Path logFile, int i) throws IOException {

        Classifier classifier = this.train(trainingData);

        String label1 = classifier.getLabelAlphabet().lookupLabel(0).toString();
        String label2 = classifier.getLabelAlphabet().lookupLabel(1).toString();
        Double currentAccuracy =  classifier.getAccuracy(testingData);

        Files.write(logFile, ("The "+ i +"th testing accuracy is: " + currentAccuracy + "\r\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        Files.write(logFile, ("The "+ i +"th testing F1 for "+label1+" is: " + classifier.getF1(testingData, 0)+ "\r\n").getBytes(), StandardOpenOption.APPEND);
        Files.write(logFile, ("The "+ i +"th testing F1 for "+label2+" is: " + classifier.getF1(testingData, 1)+ "\r\n").getBytes(), StandardOpenOption.APPEND);
        Files.write(logFile, "\r\n".getBytes(), StandardOpenOption.APPEND);

        if (currentAccuracy > bestAccuracy) {
            bestAccuracy = currentAccuracy;
            bestClassifier = classifier;
        }
        numOfTrials++;
        accuracyEachTrial.add(currentAccuracy);

        return classifier;
    }

    public double getAvgAccuracy() {
        double sum = 0;
        for (Double num : accuracyEachTrial) {
            sum = sum + num;
        }

        System.out.println("Total trial number is: " + numOfTrials);

        System.out.println("Combined accuracy is: " + (sum / (double) numOfTrials));

        System.out.println();

        return sum / (double) numOfTrials;
    }

    public Classifier train(InstanceList instancesTraining) throws IOException {
        return trainer.train(instancesTraining);
    }
}
