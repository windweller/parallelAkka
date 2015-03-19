package classification.ngrams;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class NgramTest {

    @Test
    public void testUnigram()
    {
        Unigram unigram = new Unigram();
        unigram.add("A", 1); unigram.add("B", 2);
        unigram.add("B", 4); unigram.add("C", 3);
        unigram.add("C", 6); unigram.add("D", 4);

        unigram.estimateMaximumLikelihoods();

        assertEquals(0.05, unigram.getLikelihood("A"), 0);
        assertEquals(0.3 , unigram.getLikelihood("B"), 0);
        assertEquals(0.45, unigram.getLikelihood("C"), 0);
        assertEquals(0.2 , unigram.getLikelihood("D"), 0);
        assertEquals(0 , unigram.getLikelihood("E"), 0);

        Map.Entry<String, Double> p = unigram.getBest();
        assertEquals("C" , p.getKey());
        assertEquals(0.45, p.getValue(), 0);
    }

    @Test
    public void testBigram()
    {
        Bigram bigram = new Bigram();

        bigram.add("A", "A1", 2);
        bigram.add("A", "A2", 3);
        bigram.add("B", "B1", 1);
        bigram.add("B", "B2", 4);
        bigram.estimateMaximumLikelihoods();

        assertEquals(0.4, bigram.getLikelihood("A","A1"), 0);
        assertEquals(0.6, bigram.getLikelihood("A","A2"), 0);
        assertEquals(0.2, bigram.getLikelihood("B","B1"), 0);
        assertEquals(0.8, bigram.getLikelihood("B","B2"), 0);
        assertEquals(0  , bigram.getLikelihood("A","A0"), 0);
        assertEquals(0  , bigram.getLikelihood("C","A1"), 0);

        Map.Entry<String,Double> p;

        p = bigram.getBest("A");
        assertEquals("A2", p.getKey());
        assertEquals(0.6 , p.getValue(), 0);

        p = bigram.getBest("B");
        assertEquals("B2", p.getKey());
        assertEquals(0.8 , p.getValue(), 0);
    }

    @Test
    public void testTrigram()
    {
        Trigram trigram = new Trigram();
        //

    }
}