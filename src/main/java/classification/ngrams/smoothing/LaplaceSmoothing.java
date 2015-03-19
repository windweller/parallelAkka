package classification.ngrams.smoothing;

import classification.ngrams.Bigram;
import classification.ngrams.Unigram;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by anie on 2/19/2015.
 */
public class LaplaceSmoothing implements ISmoothing {

    private double d_unseenLikelihood;
    private double d_alpha;

    public LaplaceSmoothing(double alpha) {
        this.d_alpha = alpha;
    }

    @Override
    public void estimateMaximumLikelihoods(Unigram unigram) {
        Map<String, Long> countMap = unigram.getCountMap();
        double t = d_alpha * countMap.size() + unigram.getTotalCount();
        Map<String, Double> map = countMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> (d_alpha + entry.getValue())/t));
        unigram.setLikelihoodMap(map);
        d_unseenLikelihood = d_alpha / t;
    }

    @Override
    public void estimateMaximumLikelihoods(Bigram bigram) {
        Map<String, Unigram> unigramMap = bigram.getUnigramMap();

        for (Unigram unigram : unigramMap.values())
            unigram.estimateMaximumLikelihoods();

        d_unseenLikelihood = 1d / bigram.getWordSet().size();
    }

    @Override
    public double getUnseenLikelihood() {
        return d_unseenLikelihood;
    }

    @Override
    public ISmoothing createInstance() {
        return new LaplaceSmoothing(d_alpha);
    }
}
