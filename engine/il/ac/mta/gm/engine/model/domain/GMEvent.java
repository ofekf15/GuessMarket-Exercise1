package il.ac.mta.gm.engine.model.domain;

import il.ac.mta.gm.engine.model.lmsr.LMSRMarket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GMEvent {
    private final int id;
    private final String name;
    private final String description;
    private final Commission commission;
    private final GMOption[] options;
    private final LMSRMarket market;
    
    private EventStatus status;
    private Integer winningOptionIndex;
    
    private final EventAccount account;
    private final List<TradeRecord> tradeHistory;

    public GMEvent(int id, String name, String description, Commission commission, GMOption[] options, int b) {
        if (options == null || options.length != 2) {
            throw new IllegalArgumentException("Event must have exactly 2 options.");
        }
        
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.options = options;
        this.market = new LMSRMarket(b);
        
        this.status = EventStatus.ACTIVE;
        
        // Initial balance = C(0,0) = b * ln(2)
        double initialBalance = this.market.computeC();
        this.account = new EventAccount(initialBalance);
        this.tradeHistory = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Commission getCommission() { return commission; }
    public GMOption[] getOptions() { return options.clone(); }
    public LMSRMarket getMarket() { return market; }
    
    public EventStatus getStatus() { return status; }

    public Integer getWinningOptionIndex() { return winningOptionIndex; }

    public void closeWithWinner(int winningOptionIndex) {
        if (this.status == EventStatus.CLOSED) {
            throw new IllegalStateException("Event is already CLOSED.");
        }
        if (winningOptionIndex < 0 || winningOptionIndex >= options.length) {
            throw new IllegalArgumentException("Invalid winning option index: " + winningOptionIndex);
        }
        if (this.winningOptionIndex != null) {
            throw new IllegalStateException("Winner is already set.");
        }
        this.status = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex;
    }
    
    public EventAccount getAccount() { return account; }
    
    public List<TradeRecord> getTradeHistory() { 
        return Collections.unmodifiableList(tradeHistory); 
    }
    
    public void addTradeRecord(TradeRecord record) {
        this.tradeHistory.add(record);
    }
}
