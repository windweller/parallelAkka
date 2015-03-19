package classification.subtree;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.*;
import net.didion.jwnl.data.Exc;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by anie on 3/4/2015.
 * Should I do anything else with this pipe???
 */
public class DataTable2FeatureVectorAndLabel extends Pipe {

    private static final long serialVersionUID = 1L;
    private int targetCol;
    private boolean SVMFormat;

    public DataTable2FeatureVectorAndLabel(int targetCol, boolean SVMFormat) {
        super (new Alphabet(), new LabelAlphabet());
        this.targetCol = targetCol;
        this.SVMFormat = SVMFormat;
    }

    @Override
    public Instance pipe(Instance carrier) {

        //this contains targetColumn
        String[] dataArray = (String[]) carrier.getData();

        Label label = ((LabelAlphabet)getTargetAlphabet()).lookupLabel(dataArray[targetCol], true);
        carrier.setTarget(label);

        double[] valuesArray = new double[dataArray.length - 1];

        //remove target here
        int j = 0;
        int k = 0;
        while (j < dataArray.length) {
            if (j == dataArray.length - 1 && j == targetCol) break;

            if (j == targetCol)
                j++;
            try {
                valuesArray[k] = Double.parseDouble(dataArray[j]);
            }catch (Exception e) {
                System.out.println("target column: " +targetCol);
                System.out.println("array length: " +dataArray.length);
                System.exit(0);
            }

            k++;
            j++;
        }

        int[] indices = new int[valuesArray.length];

        for (int i = 0; i < valuesArray.length; i++) {
            //here, index is used as "feature", which is true
            int index = getDataAlphabet().lookupIndex(i, true);
            //no need to check -1 because we don't stop growth
            indices[i] = index;
        }

        FeatureVector fv = new FeatureVector(getDataAlphabet(), indices, valuesArray);
        carrier.setData(fv);

        return carrier;
    }
}
