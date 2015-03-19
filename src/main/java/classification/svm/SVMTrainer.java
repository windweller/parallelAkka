package classification.svm;

import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SparseVector;
import libsvm.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by anie on 2/23/2015.
 */
public class SVMTrainer extends ClassifierTrainer<LibSVM> {

    private svm_parameter param;
    private svm_problem prob;
    private svm_model model;
    public int max_feature_length; //so we know how large is the feature sequence

    LibSVM classifier;
    Map<String, Double> labelMap = new HashMap<String, Double>();

    public SVMTrainer(String arg) throws Exception {
        param = new svm_parameter();
        //default values
        param.svm_type = svm_parameter.C_SVC;

        switch (arg) {
            case "3":
                param.kernel_type = svm_parameter.RBF;
                break;
            case "1":
                param.kernel_type = svm_parameter.LINEAR;
                break;
            default:
                throw new Exception("method number is not matched: " + arg);
        }

        param.degree = 3;
        param.gamma = 0;
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 1;  //default doing probability estimates
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

    }

    @Override
    public LibSVM getClassifier() {
        return classifier;
    }

    @Override
    public LibSVM train(InstanceList trainingSet) {
        parse(trainingSet);
        String error_msg = svm.svm_check_parameter(prob, param);

        if (error_msg != null) {
            System.err.print("SVM error: "+error_msg+"\n");
            System.exit(0);
        }

        model = svm.svm_train(prob, param);

        return new LibSVM(trainingSet.getPipe(), model);
    }

    public LibSVM parse(InstanceList trainingSet) {

        prob = new svm_problem();

        //could be the way to construct vector
        // trainingSet.getAlphabets().length;
        Vector<Double> vy = new Vector<>();       //vector for y axis
        Vector<svm_node[]> vx = new Vector<svm_node[]>();  //vector for x axis

        int max_index = 0;

        for (Instance instance : trainingSet) {

            Double yvalue = labelMap.computeIfAbsent(String.valueOf(instance.getTarget()),
                    key -> labelMap.size() + 1.0);

            //output.append(((SparseVector) instance.getData()).toString(true));

            SparseVector sp = (SparseVector) instance.getData();

            vy.addElement(yvalue);

            double[] values = sp.getValues();
            int[] indices = sp.getIndices();

            svm_node[] x = new svm_node[values.length];

            for (int i = 0; i < sp.getValues().length; i++) {
                x[i] = new svm_node();
                x[i].index = indices[i];
                x[i].value = values[i];
            }

            if (values.length > 0) Math.max(max_index, x[values.length - 1].index);
            vx.add(x);
        }

        prob.l = vy.size();
        prob.x = new svm_node[prob.l][];

        for (int i = 0; i<prob.l; i++)
            prob.x[i] = vx.elementAt(i);

        prob.y = new double[prob.l];

        for (int i =0; i<prob.l; i++) prob.y[i] = vy.elementAt(i);

        if (param.gamma == 0 && max_index > 0) param.gamma = 1.0/max_index;

        max_feature_length = max_index;

        return null;
    }
}
