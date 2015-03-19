package classification.subtree;

import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.InstanceList;
import classification.PipeBuilder;
import classification.file.DataFile;
import classification.file.DataTable;
import classification.file.FileType;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *  this method can be called by Main
 *  or called in GenericTrainer
 *  It provides its own pipe, data processing tool (return instanceList)
 *
 *  Generic Trainer has the best platform to run Trial,
 *  and multiple algorithms
 */
public class SubtreePipeBuilder implements PipeBuilder {

    public Pipe pipe;
    int idColumn;
    int targetColumn;
    boolean header;
    boolean SVMFormat;

    public SubtreePipeBuilder(int idColumn, int targetColumn, boolean header, boolean SVMFormat) {
        this.idColumn = idColumn;
        this.targetColumn = targetColumn;
        this.header = header;
        this.SVMFormat = SVMFormat;
        pipe = buildPipe();
    }

    public Pipe buildPipe() {
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        pipeList.add(new DataTable2FeatureVectorAndLabel(targetColumn, SVMFormat));

        return new SerialPipes(pipeList);
    }

    public InstanceList readingSingleFile(String file)
            throws IOException {

        InstanceList instances = new InstanceList(pipe);

        DataTable dt = DataFile.create(FileType.CSVFile, idColumn, targetColumn, header).readIn(file).toDataTable();

        instances.addThruPipe(new DataTableIterator(dt));

        return instances;
    }

    public InstanceList readingFromDataTable(DataTable dt) {

        InstanceList instances = new InstanceList(pipe);
        instances.addThruPipe(new DataTableIterator(dt));

        return instances;
    }

}
