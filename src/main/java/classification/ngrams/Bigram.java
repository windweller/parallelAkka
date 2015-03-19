package classification.ngrams;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by anie on 2/18/2015.
 */
public class Bigram {

    private Map<String, Unigram> m_unigrams;

    public Bigram()
    {
        m_unigrams = new HashMap<String, Unigram>();
    }

    public void add(String word1, String word2, long count)
    {
        //If word1 doesn't exist, create new unigram, put as value of word1,
        //computeIfAbsent returns new Unigram, or old Unigram, depending on word1 absent or not
        //then you add word2's count to word1's unigram
        //count(Word2 | Word1)
        m_unigrams.computeIfAbsent(word1, key -> new Unigram()).add(word2, count);
    }

    public void estimateMaximumLikelihoods()
    {
        for (Unigram unigram : m_unigrams.values())
            unigram.estimateMaximumLikelihoods();
    }

    //how is word2 appearing under word1's condition (in word1's world)
    public double getLikelihood(String word1, String word2)
    {
        Unigram unigram = m_unigrams.get(word1);
        return (unigram != null) ? unigram.getLikelihood(word2) : 0d;
    }

    public Entry<String, Double> getBest(String word)
    {
        //get one unigram associated with the word, but inside that unigram
        //many words other than this "word" are in there
        Unigram unigram = m_unigrams.get(word);
        return (unigram != null) ? unigram.getBest() : null;
    }

    public List<Entry<String, Double>> getSortedList(String word)
    {
        Unigram unigram = m_unigrams.get(word);
        return (unigram != null) ? unigram.getSortedList() : new ArrayList<>();
    }

    /***************Helper Functions ******************/

    /** @return the map whose keys and values are words and their unigram maps. */
    public Map<String,Unigram> getUnigramMap()
    {
        return m_unigrams;
    }

    public boolean contains(String word1, String word2)
    {
        Unigram unigram = m_unigrams.get(word1);
        return (unigram != null) && unigram.contains(word2);
    }

    public Set<String> getWordSet()
    {
        Set<String> set = new HashSet<>();

        for (Entry<String,Unigram> entry : m_unigrams.entrySet())
        {
            set.add(entry.getKey());
            set.addAll(entry.getValue().getCountMap().keySet());
        }

        return set;
    }

}
