package classification.svm;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.*;
import libsvm.*;

public class LibSVM extends Classifier {

    public svm_model model;
    int nr_class;
    int svm_type;

    int correct = 0;
    int total = 0;
    double error = 0;
    double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;


    public LibSVM(Pipe pipe,svm_model model) {

        super(pipe);

        this.model = model;
        nr_class=svm.svm_get_nr_class(model);
        svm_type=svm.svm_get_svm_type(model);
    }

    @Override
    public Classification classify(Instance instance) {

//        int numClasses = getLabelAlphabet().size();

        int[] labels=new int[nr_class];
        svm.svm_get_labels(model,labels);
        double[] prob_estimates = new double[nr_class];

        //parsing

        SparseVector sp = (SparseVector) instance.getData();
        double[] values = sp.getValues();
        int[] indices = sp.getIndices();

        svm_node[] x = new svm_node[values.length];

        for (int i = 0; i < sp.getValues().length; i++) {
            x[i] = new svm_node();
            x[i].index = indices[i];
            x[i].value = values[i];
        }

        double v = svm.svm_predict_probability(model,x,prob_estimates);

        //now we test for accuracy

        return new Classification(instance, this, new LabelVector(getLabelAlphabet(), prob_estimates));
    }

    //It works, but maybe the label isn't working so well
    public double getAccuracy (InstanceList ilist) {

        return new Trial(this, ilist).getAccuracy();
    }

    //everything else is not implemented yet
    //Right now precision doesn't work for getF1 on label 2
    //JVM printed error:
    //WARNING: No examples with predicted label NANFuture!
    public double getPrecision (InstanceList ilist, int index) { return new Trial(this, ilist).getPrecision(index); }
    public double getPrecision (InstanceList ilist, Labeling labeling) { return new Trial(this, ilist).getPrecision(labeling); }
    public double getPrecision (InstanceList ilist, Object labelEntry) { return new Trial(this, ilist).getPrecision(labelEntry); }
    public double getRecall (InstanceList ilist, int index) { return new Trial(this, ilist).getRecall(index); }
    public double getRecall (InstanceList ilist, Labeling labeling) { return new Trial(this, ilist).getRecall(labeling); }
    public double getRecall (InstanceList ilist, Object labelEntry) { return new Trial(this, ilist).getRecall(labelEntry); }
    public double getF1 (InstanceList ilist, int index) { return new Trial(this, ilist).getF1(index); }
    public double getF1 (InstanceList ilist, Labeling labeling) { return new Trial(this, ilist).getF1(labeling); }
    public double getF1 (InstanceList ilist, Object labelEntry) { return new Trial(this, ilist).getF1(labelEntry); }
    public double getAverageRank (InstanceList ilist) { return new Trial(this, ilist).getAverageRank(); }
}
