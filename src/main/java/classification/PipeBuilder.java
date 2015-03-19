package classification;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by anie on 3/5/2015.
 */
public interface PipeBuilder {

    public Pipe buildPipe();
    public InstanceList readingSingleFile(String file) throws IOException;

}
