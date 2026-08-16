package il.ac.mta.gm.engine.model.domain;

public class EventAccount {
    private double balance;
    private double totalFeesCollected;

    public EventAccount(double initialBalance) {
        this.balance = initialBalance;
        this.totalFeesCollected = 0.0;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalFeesCollected() {
        return totalFeesCollected;
    }

    public void applyPurchase(double sharesCost, double commissionPaid, CommissionType commissionType) {
        this.balance += sharesCost;
        if (commissionType == CommissionType.ON_PURCHASE) {
            this.balance += commissionPaid;
            this.totalFeesCollected += commissionPaid;
        }
    }

    public void applySettlement(double grossPayout, double commissionPaid) {
        this.balance += commissionPaid;
        this.balance -= grossPayout;
        this.totalFeesCollected += commissionPaid;
    }
}
