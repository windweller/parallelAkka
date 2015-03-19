package classification.ngrams.smoothing;

import classification.ngrams.Bigram;
import classification.ngrams.Unigram;

/**
 * Created by anie on 2/19/2015.
 */
public interface ISmoothing {

    /**
     * Estimates the maximum likelihoods of all words in {@code unigram}.
     * @param unigram the unigram consisting of words and their counts.
     */
    void estimateMaximumLikelihoods(Unigram unigram);

    /**
     * Estimates the maximum likelihoods of all words in {@code bigram}.
     * @param bigram the bigram consisting of words and their counts.
     */
    void estimateMaximumLikelihoods(Bigram bigram);

    /** @return the likelihood of unseen word. */
    double getUnseenLikelihood();

    /** @return a new smoothing instance. */
    ISmoothing createInstance();
}
