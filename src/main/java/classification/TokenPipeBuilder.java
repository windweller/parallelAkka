package classification;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.InstanceList;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by anie on 3/5/2015.
 */
public class TokenPipeBuilder implements PipeBuilder {
    public Pipe pipe;
    public int[] gramFlag;

    public TokenPipeBuilder(int[] gramFlag) {
        pipe = buildPipe();
        this.gramFlag = gramFlag;
        if(gramFlag == null) {
            System.out.println("gramFlag can't be null!");
            System.exit(0);
        }
    }

    public Pipe buildPipe() {
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+"); //punctuations are removed

        pipeList.add(new Input2CharSequence());

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

    public InstanceList readingSingleFile(String file)
            throws FileNotFoundException, UnsupportedEncodingException {

        InstanceList instances = new InstanceList(pipe);
        Reader fileReader = new InputStreamReader(new FileInputStream(new File(file)), "UTF-8");
        instances.addThruPipe(new CsvIterator(fileReader,
                Pattern.compile("(\\w+?),(\\w+?),(.+)"), 3, 2, 1));

        return instances;
    }
}
