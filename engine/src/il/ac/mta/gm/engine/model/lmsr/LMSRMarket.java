package il.ac.mta.gm.engine.model.lmsr;

import java.util.Arrays;

/**
 * Implements the Logarithmic Market Scoring Rule (LMSR) math model for Guess Market.
 * Exactly two options are supported per event (binary event).
 */
public class LMSRMarket {

    private final int b;
    private final int[] q; // Quantity of shares purchased for each option (index 0 and 1)

    /**
     * Initializes a new LMSR market.
     * @param b The liquidity parameter. Must be a positive integer (b >= 1).
     */
    public LMSRMarket(int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter 'b' must be a positive integer (b >= 1).");
        }
        this.b = b;
        this.q = new int[2];
        this.q[0] = 0;
        this.q[1] = 0;
    }

    /**
     * Calculates the current cost function C(q0, q1) of the market.
     * Uses the log-sum-exp trick to maintain numerical stability and prevent overflow
     * when (q / b) is large.
     */
    public double computeC() {
        return computeCWithQ(this.q[0], this.q[1]);
    }

    private double computeCWithQ(int q0, int q1) {
        double x0 = (double) q0 / b;
        double x1 = (double) q1 / b;
        
        // Log-Sum-Exp trick: ln(e^x + e^y) = max + ln(e^(x-max) + e^(y-max))
        double max = Math.max(x0, x1);
        double sumExp = Math.exp(x0 - max) + Math.exp(x1 - max);
        
        return b * (max + Math.log(sumExp));
    }

    /**
     * Calculates the current price (probability) of a given option.
     * @param optionIndex 0 or 1.
     */
    public double computeOptionPrice(int optionIndex) {
        if (optionIndex < 0 || optionIndex > 1) {
            throw new IllegalArgumentException("Invalid option index. Must be 0 or 1.");
        }
        
        double x0 = (double) this.q[0] / b;
        double x1 = (double) this.q[1] / b;
        double max = Math.max(x0, x1);
        
        double exp0 = Math.exp(x0 - max);
        double exp1 = Math.exp(x1 - max);
        double sumExp = exp0 + exp1;
        
        if (optionIndex == 0) {
            return exp0 / sumExp;
        } else {
            return exp1 / sumExp;
        }
    }

    /**
     * Quotes the cost of purchasing a given quantity of shares for a specific option.
     * Does NOT mutate the state of the market.
     */
    public double quotePurchase(int optionIndex, int quantity) {
        if (optionIndex < 0 || optionIndex > 1) {
            throw new IllegalArgumentException("Invalid option index. Must be 0 or 1.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be strictly positive (quantity > 0).");
        }

        double cBefore = computeC();
        
        int simulatedQ0 = this.q[0];
        int simulatedQ1 = this.q[1];
        
        if (optionIndex == 0) {
            simulatedQ0 += quantity;
        } else {
            simulatedQ1 += quantity;
        }
        
        double cAfter = computeCWithQ(simulatedQ0, simulatedQ1);
        
        return cAfter - cBefore;
    }

    /**
     * Commits a purchase of a given quantity of shares for a specific option.
     * Mutates the state of the market.
     * @return The cost of the purchased shares.
     */
    public double commitPurchase(int optionIndex, int quantity) {
        double cost = quotePurchase(optionIndex, quantity);
        if (quantity > 0) {
            this.q[optionIndex] += quantity;
        }
        return cost;
    }

    public int getB() {
        return b;
    }

    public int[] getQ() {
        return Arrays.copyOf(this.q, this.q.length);
    }
}
