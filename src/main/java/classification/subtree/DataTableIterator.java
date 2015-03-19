package classification.subtree;

import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import classification.file.DataTable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.*;

/**
 * This iterator creates an array of string
 * data is String[]
 * target is label, based on targetColumn
 * This Iterator does not handle  "1:1 2:1" compact format
 */
public class DataTableIterator implements Iterator<Instance> {

    int index = -1;
    DataTable dataTable;
    Iterator<Entry<String, ArrayList<String>>> it;


    public DataTableIterator(DataTable dt) {
        dataTable = dt;
        it = dt.content.entrySet().iterator();
    }

    @Override
    public Instance next() {

        Entry<String, ArrayList<String>> entry = it.next();

        //this is Mallet's quirkiness
        URI uri = null;
        try { uri = new URI (entry.getKey()); }
        catch (URISyntaxException e) {e.printStackTrace();}

        //data: String[], target, uri
        Instance inst = new Instance (null, null, uri, null);

        String[] dataVectors = new String[entry.getValue().size()];

        for (int i = 0; i < dataVectors.length; i++)
                dataVectors[i] = entry.getValue().get(i);

        inst.setData(dataVectors);

        //data, target: Label, uri are all set now, hopefully
        return inst;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }
}
