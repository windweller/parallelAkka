package classification.ngrams;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by anie on 2/18/2015.
 */
public class Trigram {

    private Map<String, Bigram> m_bigrams;

    public Trigram() {
        m_bigrams = new HashMap<>();
    }

    public void add(String word1, String word2, String word3, long count) {
        //not so sure if this is the right way
        m_bigrams.computeIfAbsent(word1, key -> new Bigram()).add(word2, word3, count);
    }

    public void estimateMaximumLikelihoods()
    {
        for (Bigram bigram : m_bigrams.values())
            bigram.estimateMaximumLikelihoods();
    }

    //how is word2 appearing under word1's condition (in word1's world)
    public double getLikelihood(String word1, String word2, String word3)
    {
        Bigram bigram = m_bigrams.get(word1);
        return (bigram != null) ? bigram.getLikelihood(word2, word3) : 0d;
    }

    public Entry<String, Double> getBest(String word)
    {
        //get one unigram associated with the word, but inside that unigram
        //many words other than this "word" are in there
        Bigram bigram = m_bigrams.get(word);
        return (bigram != null) ? bigram.getBest(word) : null;
    }

    public List<Entry<String, Double>> getSortedList(String word)
    {
        Bigram bigram = m_bigrams.get(word);
        return (bigram != null) ? bigram.getSortedList(word) : new ArrayList<>();
    }

    /***************Helper Functions ******************/

    /** @return the map whose keys and values are words and their unigram maps. */
    public Map<String,Bigram> getBigramMap()
    {
        return m_bigrams;
    }

    public boolean contains(String word1, String word2, String word3)
    {
        Bigram bigram = m_bigrams.get(word1);
        return (bigram != null) && bigram.contains(word2, word3);
    }

    public Set<String> getWordSet()
    {
        Set<String> set = new HashSet<>();

        for (Entry<String,Bigram> entry : m_bigrams.entrySet())
        {
            set.add(entry.getKey());
            set.addAll(entry.getValue().getWordSet()); //not sure if this is the right way...
        }

        return set;
    }
}
