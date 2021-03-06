package FolderReadingNIO;

import edu.stanford.nlp.ling.HasWord;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class CollectionHandler {

    public static List<List<HasWord>> construct2dList() {
        return new ArrayList<List<HasWord>>();
    }

    public static ArrayList<String> buildListStringFromListHasWord(List<List<HasWord>> listHasWord) {

        ArrayList<String> sentenceList = new ArrayList<String>();

        for (List<HasWord> sentence : listHasWord) {

            StringBuilder sentenceSb = new StringBuilder();

            for (HasWord token : sentence) {
                if (sentenceSb.length() > 1) {
                    sentenceSb.append(" ");
                }
                sentenceSb.append(token);
            }

            sentenceList.add(sentenceSb.toString());
        }

        return sentenceList;
    }
}
