package classification.ngrams;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Created by anie on 2/18/2015.
 */
public class Unigram {

    private Map<String, Double> m_likelihoods;
    private Map<String, Long> m_counts;
    private long t_counts;


    public Unigram()
    {
        m_counts = new HashMap<>();
        t_counts = 0;
    }

    public void add(String word, long count)
    {
        t_counts += count;
        //if word nonexist, use "count"; if exist, update by oldCount - got from old value,
        // plus newCount - got by the "count".
        m_counts.merge(word, count, (oldCount, newCount) -> oldCount + newCount);
    }

    //take frequency counts in a hashmap, transform it into another map
    public void estimateMaximumLikelihoods()
    {
        m_likelihoods = m_counts.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue() / t_counts
        ));
    }

    public double getLikelihood(String word)
    {
        return m_likelihoods.getOrDefault(word, 0d);
    }

    public Entry<String, Double> getBest()
    {
       // why not THIS!?
       // return m_likelihoods.getOrDefault(null, Collections.max(m_likelihoods.entrySet(), Entry.comparingByValue()));
        return m_likelihoods.isEmpty() ? null
                : Collections.max(m_likelihoods.entrySet(), Entry.comparingByValue());
    }

    public List<Entry<String, Double>> getSortedList()
    {
        List<Entry<String, Double>> list = new ArrayList<>(m_likelihoods.entrySet());
        Collections.sort(list, Entry.comparingByValue(Collections.reverseOrder())); //because you want highest first? Probably
        return list;
    }

    /************** Helper functions ************/
    public long getTotalCount()
    {
        return t_counts;
    }

    /** @return the map whose keys and values are the words and their counts. */
    public Map<String,Long> getCountMap()
    {
        return m_counts;
    }

    public Map<String,Double> getLikelihoodMap()
    {
        return m_likelihoods;
    }

    /** Assigns the likelihood map to this unigram. */
    public void setLikelihoodMap(Map<String,Double> map)
    {
        m_likelihoods = map;
    }

    public boolean contains(String word)
    {
        return m_likelihoods.containsKey(word);
    }

}
