package classification.ngrams.smoothing;

import classification.ngrams.Bigram;
import classification.ngrams.Unigram;

/**
 * Created by anie on 2/19/2015.
 */
public class DiscountSmoothing implements ISmoothing {

    private double d_unseenLikelihood;
    private double d_alpha;

    public DiscountSmoothing(double alpha) {
        d_alpha = alpha;
    }

    @Override
    public ISmoothing createInstance() {
        return new DiscountSmoothing(d_alpha);
    }

    @Override
    public void estimateMaximumLikelihoods(Unigram unigram) {

    }

    @Override
    public void estimateMaximumLikelihoods(Bigram bigram) {

    }

    @Override
    public double getUnseenLikelihood() {
        return 0;
    }
}
