package classification;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import ch.qos.logback.core.net.SyslogOutputStream;
import classification.subtree.SubtreePipeBuilder;
import classification.svm.SVMTrainer;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class Main {

    public static void main(String [ ] args) throws Exception {
        Parser parser = new GnuParser();
        Options options = new Options();

        options.addOption("t", "trainClassifier", false, "train a classifier with proper input file and saving path");
        options.addOption("tf", "trainingFileLoc", true, "specify a file that's used for training");
        options.addOption("g", "ngrams", true, "specify whether the unigram, bigram, or trigram model.");
        options.addOption("st", "saveTest", true, "specify whether you want to save testing file. Must be specified if n-fold is enabled.");
        options.addOption("str", "saveTrain", true, "specify whether you want to save training file.");
        options.addOption("s", "classifierSavingPath", true, "specify a classifier's saving path");
        options.addOption("spl", "splitPortion", true, "Specify how much percentage wanted for training");
        options.addOption("cvl", "crossValidation", true, "Specify n-fold cross validation");
        options.addOption("cvll", "crossValidationLogFile", true, "Specify location of a logger file");
        options.addOption("cvlld", "crossValidationLogDir", true, "Specify directory of where logger files should be put");
        options.addOption("cvllo", "crossValidationNFoldDir", true, "Specify directory of where nfold traing/testing pair should be saved");

        options.addOption("maxEnt", "maximumEntropy", false, "flag to add maximum entropy Classifier");
        options.addOption("nb", "naiveBayes", false, "flag to add naive Bayes Classifier");
        options.addOption("svm", "libSVM", true, "flag to add libSVM classifier, take in parameters flags 1:3, 1-linear, 3-radial");

        options.addOption("c", "classify", false, "flag if used to classify");
        options.addOption("l", "loadTrainedClassifier", true, "load a previously trained classifier to program");
        options.addOption("li", "loadClassifyFile", true, "load a file to be classified");
        options.addOption("o", "outputLocation", true, "specify an output file location, must be directory now, to SAVE classifiers");

        options.addOption("sp", "splitCSVFile", true, "specify a CSV file to split");
        options.addOption("so", "splitOutputFile", true, "specify a destination for outputed file");
        options.addOption("id", "idColumn", true, "specify a column number for id");
        options.addOption("sen", "senColumn", true, "specify a column number for sen");
        options.addOption("cc", "criteriaColumn", true, "specify a column number for criteria column");
        options.addOption("cr", "criteria", true, "specify an integer criteria to split CSV");
        options.addOption("ll", "label", true, "specify a label name for csv sentences");

        options.addOption("dv", "dataVector", true, "specify a data vector file to provide extra features. Will" +
                "kick start the second run");
        options.addOption("dvh", "dataVectorHeader", false, "specify whether data vector file has a header");
        options.addOption("idta", "idTarget", true, "specify an id and target column for data being read in, format id:target");
        options.addOption("cdrop", "colDrop", true, "specify column number to drop, format col1:col2:col3");


        CommandLine cmd = null;
        try {cmd = parser.parse(options, args);} catch(ParseException e) {usage(options);}

        if (cmd != null && cmd.hasOption("c")) {
            if (!cmd.hasOption("l")) {
                System.out.println("must specify classifier location");
                System.exit(0);
            }
            if (!cmd.hasOption("li")) {
                System.out.println("must specify a file that's to be classified");
                System.exit(0);
            }
            if (!cmd.hasOption("o")) {
                System.out.println("must specify a location for output file");
                System.exit(0);
            }
            File classifierLoc = new File(cmd.getOptionValue("l"));
            File inputFile = new File(cmd.getOptionValue("li"));
            File outputFile = new File(cmd.getOptionValue("o"));

            Classifier classifier = MME.load(classifierLoc);
            MME.classify(classifier, inputFile, outputFile);
        }

        if (cmd != null && cmd.hasOption("sp")) {
            if (!cmd.hasOption("cc")) {
                System.out.println("must specify criteria column.");
                System.exit(0);
            }

            if (!cmd.hasOption("id")) {
                System.out.println("must specify id column.");
                System.exit(0);
            }

            if (!cmd.hasOption("sen")) {
                System.out.println("must specify sen column.");
                System.exit(0);
            }

            if (!cmd.hasOption("cr")) {
                System.out.println("must specify classifying criteria.");
                System.exit(0);
            }

            int cc = 0;
            int sen = 0;
            int id = 0;
            final String classPositive = cmd.hasOption("ll") ? cmd.getOptionValue("ll") : "classPositive";

            try {
                cc = Integer.parseInt(cmd.getOptionValue("cc"));
                sen = Integer.parseInt(cmd.getOptionValue("sen"));
                id = Integer.parseInt(cmd.getOptionValue("id"));
                int crTest = Integer.parseInt(cmd.getOptionValue("cr"));
            }
            catch(NumberFormatException e) {System.out.println("Criteria, sentence, or id has to be an integer");
                System.out.println(e.toString()); System.exit(0);}

            if (!cmd.hasOption("so")) {
                System.out.println("must specify output file location.");
                System.exit(0);
            }

            final int cr = Integer.parseInt(cmd.getOptionValue("cr"));

            CSV csv = new CSV(cmd.getOptionValue("sp"), cmd.getOptionValue("so"), id, sen, cc);
            Function<Integer, String> func = p -> {
                if (p >= cr) {
                    return classPositive;
                }else return "NAN"+classPositive;
            };
            csv.transform(func);
        }

        /*for training */
        if (cmd != null && cmd.hasOption("t")) {

            if (!cmd.hasOption("g")) {
                System.out.println("must specify a gram option: uni, bi or tri");
                System.exit(0);
            }

            int gramFlag[];
            switch (cmd.getOptionValue("g")) {
                case "tri":
                    gramFlag = new int[] {3,2,1};
                break;
                case "bi":
                    gramFlag = new int[] {2,1};
                break;
                case "uni":
                    gramFlag = new int[] {1};
                break;
                default: gramFlag = new int[] {3,2,1};
                break;
            }

            if (!cmd.hasOption("tf")) {
                System.out.println("Specify where the file that's used for training");
                System.exit(0);
            }

            if (!cmd.hasOption("o")) {
                System.out.println("Specify the output file location, which will be in .ser form");
                System.exit(0);
            }

            if (!cmd.hasOption("spl")) {
                System.out.println("Specify how much percentage wanted for training between 0 and 1");
                System.exit(0);
            }

            if (cmd.hasOption("cvl")) {
                if (!cmd.hasOption("cvll")
                        && !cmd.hasOption("cvlld")
                        && !cmd.hasOption("cvllo")
                        && !cmd.hasOption("st")) {
                    System.out.println("When using cross-validation," +
                            " must specify logger file path or directory or saving test dir");
                    System.exit(0);
                }
            }

            String savingTest = cmd.hasOption("st") ? cmd.getOptionValue("st") : null;
            File savingTrain = cmd.hasOption("str") ? new File(cmd.getOptionValue("str")) : null;

            int crossValidation = cmd.hasOption("cvl") ? Integer.parseInt(cmd.getOptionValue("cvl")) : 0;
            String loggerPath = cmd.hasOption("cvll") ? cmd.getOptionValue("cvll") : null;
            String loggerDir = cmd.hasOption("cvlld") ? cmd.getOptionValue("cvlld") : null;

            if (loggerDir != null && !loggerDir.endsWith("\\")) {loggerDir += "\\";}
            if (savingTest != null && !savingTest.endsWith("\\")) {savingTest += "\\";}

            if (!cmd.hasOption("maxEnt") && !cmd.hasOption("nb") && !cmd.hasOption("svm")) {
                System.out.println("You must specify a training method! Flag maxEnt, nb or svm!");
                System.exit(0);
            }

            GenericTrainer classifier = new GenericTrainer(Double.parseDouble(cmd.getOptionValue("spl")),crossValidation);

            classifier.addPipeBuilder(new TokenPipeBuilder(gramFlag));

            classifier.gramFlag = gramFlag;

            boolean header = false;
            int idColumn = -1;
            int targetColumn = -1;
            String featureFile = null;

            //flag dv, we will run the second stage (additional features)
            //right now it's not activated
            if (cmd.hasOption("dv")) {

                if (!cmd.hasOption("idta")) {
                    System.out.println("Must include id column and target column in id:target format");
                    System.exit(0);
                }

                featureFile = cmd.getOptionValue("dv");

                header = cmd.hasOption("dvh"); //flag it, then we have header

                String[] pieces = cmd.getOptionValue("dita").split(":");

                if (pieces.length > 2) {
                    System.out.println("can only be two numbers i.e. 5:15");
                    System.exit(0);
                }

                idColumn = Integer.parseInt(pieces[0]);
                targetColumn = Integer.parseInt(pieces[1]);

                classifier.addPipeBuilder(new SubtreePipeBuilder(idColumn, targetColumn, header, false));
            }

            int[] dropCols = null;

            if (cmd.hasOption("cdrop")) {
                String[] dropColsString = cmd.getOptionValue("cdrop").split(":");
                dropCols = new int[dropColsString.length];
                for (int i = 0; i < dropColsString.length; i++)
                    dropCols[i] = Integer.parseInt(dropColsString[i]);
            }

            String savingDir = cmd.hasOption("o") ? cmd.getOptionValue("o") : null; //"E:\\Allen\\TwitterProject\\Pilot2\\AnxietyClassifier.ser"
            if (savingDir != null && !savingDir.endsWith("\\")) {savingDir += "\\";}

            if (cmd.hasOption("maxEnt")) {
                classifier.addClassifierTrainers(new MaxEntTrainer());
                classifier.addLoggerFiles(loggerDir + "maxEntTiralLogger.txt"); //method inside will take care of trailing slash
                classifier.addSavingDir(savingDir+"maxEntClassifier.ser");
                classifier.addSavingTest(savingTest+"maxEntTest.txt");
            }

            if (cmd.hasOption("nb")) {
                classifier.addClassifierTrainers(new NaiveBayesTrainer()); //same as Mallet's default choice
                classifier.addLoggerFiles(loggerDir + "naiveBayesTiralLogger.txt");
                classifier.addSavingDir(savingDir+"naiveBayesClassifier.ser");
                classifier.addSavingTest(savingTest+"naiveBayesTest.txt");
            }

            if (cmd.hasOption("svm")) {
                String[] kernelMethods = cmd.getOptionValue("svm").split(":");

                for (String method: kernelMethods) {
                    classifier.addClassifierTrainers(new SVMTrainer(method)); //this will throw fatal error if method doesn't match
                    classifier.addLoggerFiles(loggerDir + "SVMTiralLogger"+method+".txt");
                    classifier.addSavingDir(savingDir+"SVMClassifier"+method+".ser"); //emmmm, may not really work on loading, saving should be fine
                    classifier.addSavingTest(savingTest+"svmTest"+method+".txt");
                }
            }

            if (!classifier.isClassifierEmpty()) {
                String file = cmd.getOptionValue("tf"); //"E:\\Allen\\TwitterProject\\Pilot2\\AnxietyTrainingCorpus.txt"
                String nFoldSave = cmd.hasOption("cvllo") ? cmd.getOptionValue("cvllo") : null;
                classifier.train(file, savingTrain, nFoldSave, featureFile, idColumn, targetColumn, header, dropCols);
            }
            else {
                System.out.println("You didn't specify any classifier");
                System.exit(0);
            }
        }
    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Mallet Classifier", options);
    }

}
