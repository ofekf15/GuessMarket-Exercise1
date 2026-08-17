package il.ac.mta.gm.engine.core;

import il.ac.mta.gm.engine.api.EngineInterface;
import il.ac.mta.gm.dto.EventDisplayDTO;
import il.ac.mta.gm.dto.EventStatusDTO;
import il.ac.mta.gm.dto.OptionStatusDTO;
import il.ac.mta.gm.dto.TradeRecordDTO;
import il.ac.mta.gm.dto.TradeResultDTO;
import il.ac.mta.gm.dto.EventStatus;
import il.ac.mta.gm.dto.CommissionType;
import il.ac.mta.gm.engine.model.domain.GMEvent;
import il.ac.mta.gm.engine.model.domain.GMOption;
import il.ac.mta.gm.engine.model.domain.TradeRecord;

import java.util.ArrayList;
import java.util.List;
import il.ac.mta.gm.engine.xml.XmlLoader;

public class GuessMarketEngine implements EngineInterface {

    private final SystemState state;
    private final XmlLoader xmlLoader;

    public GuessMarketEngine(SystemState state) {
        this.state = state;
        this.xmlLoader = new XmlLoader();
    }

    @Override
    public void loadSystemData(String filePath) {
        List<GMEvent> candidateEvents = xmlLoader.load(filePath);
        state.setEvents(candidateEvents);
    }

    @Override
    public List<EventDisplayDTO> getEvents() {
        List<EventDisplayDTO> dtos = new ArrayList<>();
        for (GMEvent event : state.getEvents()) {
            List<String> optionNames = new ArrayList<>();
            for (GMOption option : event.getOptions()) {
                optionNames.add(option.getName());
            }
            dtos.add(new EventDisplayDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommission().getPercentage(),
                CommissionType.valueOf(event.getCommission().getType().name()), // Mapped to DTO enum
                optionNames,
                EventStatus.valueOf(event.getStatus().name()) // Mapped to DTO enum
            ));
        }
        return dtos;
    }

    @Override
    public EventStatusDTO getEventStatus(int eventId) {
        GMEvent event = state.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }

        List<OptionStatusDTO> optionDtos = new ArrayList<>();
        GMOption[] options = event.getOptions();
        int[] q = event.getMarket().getQ();
        
        for (int i = 0; i < options.length; i++) {
            optionDtos.add(new OptionStatusDTO(
                options[i].getName(),
                event.getMarket().computeOptionPrice(i),
                q[i]
            ));
        }

        List<TradeRecordDTO> historyDtos = new ArrayList<>();
        List<TradeRecord> history = event.getTradeHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            TradeRecord r = history.get(i);
            historyDtos.add(new TradeRecordDTO(
                r.getOptionName(), r.getQuantity(), r.getSharesCost(), 
                r.getCommissionPaid(), r.getTotalPaid()
            ));
        }

        String winningOptionName = null;
        if (event.getWinningOptionIndex() != null) {
            winningOptionName = event.getOptions()[event.getWinningOptionIndex()].getName();
        }

        return new EventStatusDTO(
            event.getId(),
            event.getName(),
            EventStatus.valueOf(event.getStatus().name()), // Mapped to DTO enum
            optionDtos,
            event.getAccount().getBalance(),
            event.getAccount().getTotalFeesCollected(),
            historyDtos,
            winningOptionName
        );
    }

    @Override
    public TradeResultDTO buyShares(int eventId, int optionIndex, int quantity) {
        // 1. Validate Event exists
        GMEvent event = state.getEventById(eventId);
        if (event == null) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.EVENT_NOT_FOUND, 
                eventId, optionIndex, null, "Event not found with ID: " + eventId);
        }

        // 2. Validate Event is ACTIVE
        if (event.getStatus() != il.ac.mta.gm.engine.model.domain.EventStatus.ACTIVE) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.EVENT_NOT_ACTIVE, 
                eventId, optionIndex, "status=" + event.getStatus(), "Event is not ACTIVE.");
        }

        // 3. Validate Option Index
        if (optionIndex < 0 || optionIndex > 1) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.INVALID_OPTION, 
                eventId, optionIndex, "index=" + optionIndex, "Invalid option index.");
        }

        // 4. Validate Quantity
        if (quantity <= 0) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.INVALID_QUANTITY, 
                eventId, optionIndex, "quantity=" + quantity, "Quantity must be strictly positive.");
        }

        // 5. Quote Purchase (Calculates sharesCost without mutating q)
        double sharesCost = event.getMarket().quotePurchase(optionIndex, quantity);
        
        // 6. Calculate Commission and Total Paid
        double commissionPaid = 0.0;
        if (event.getCommission().getType() == il.ac.mta.gm.engine.model.domain.CommissionType.ON_PURCHASE) {
            commissionPaid = sharesCost * (event.getCommission().getPercentage() / 100.0);
        }
        double totalPaid = sharesCost + commissionPaid;
        
        // 7. Prepare TradeRecord Data
        String optionName = event.getOptions()[optionIndex].getName();
        TradeRecord record = new TradeRecord(optionName, quantity, sharesCost, commissionPaid, totalPaid);

        // --- All validation and calculation completed. State mutation begins exactly once here. ---
        
        event.getMarket().commitPurchase(optionIndex, quantity);
        event.getAccount().applyPurchase(sharesCost, commissionPaid, event.getCommission().getType());
        event.addTradeRecord(record);
        
        // --- State mutation complete ---
        
        return new TradeResultDTO(eventId, optionName, quantity, sharesCost, commissionPaid, totalPaid);
    }

    @Override
    public il.ac.mta.gm.dto.CloseEventResultDTO closeEvent(int eventId, int winningOptionIndex) {
        // 1. Validate Event exists
        GMEvent event = state.getEventById(eventId);
        if (event == null) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.EVENT_NOT_FOUND, 
                eventId, winningOptionIndex, null, "Event not found with ID: " + eventId);
        }

        // 2. Validate Event is ACTIVE
        if (event.getStatus() == il.ac.mta.gm.engine.model.domain.EventStatus.CLOSED) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.EVENT_ALREADY_CLOSED, 
                eventId, winningOptionIndex, "status=" + event.getStatus(), "Event is already CLOSED.");
        }
        
        if (event.getStatus() != il.ac.mta.gm.engine.model.domain.EventStatus.ACTIVE) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.EVENT_NOT_ACTIVE, 
                eventId, winningOptionIndex, "status=" + event.getStatus(), "Event is not ACTIVE.");
        }

        // 3. Validate Option Index
        if (winningOptionIndex < 0 || winningOptionIndex > 1) {
            throw new il.ac.mta.gm.engine.exception.TradingException(
                il.ac.mta.gm.engine.exception.TradingErrorCode.INVALID_OPTION, 
                eventId, winningOptionIndex, "index=" + winningOptionIndex, "Invalid option index.");
        }

        // 4. Calculate payouts
        int winningShares = event.getMarket().getQ()[winningOptionIndex];
        double grossPayout = winningShares * 1.0;
        
        double commissionPaid = 0.0;
        if (event.getCommission().getType() == il.ac.mta.gm.engine.model.domain.CommissionType.ON_CLOSE) {
            commissionPaid = grossPayout * (event.getCommission().getPercentage() / 100.0);
        }
        
        double netPayout = grossPayout - commissionPaid;
        String optionName = event.getOptions()[winningOptionIndex].getName();

        // --- All validation and calculation completed. State mutation begins exactly once here. ---
        
        event.closeWithWinner(winningOptionIndex);
        event.getAccount().applySettlement(grossPayout, commissionPaid);
        
        // --- State mutation complete ---
        
        return new il.ac.mta.gm.dto.CloseEventResultDTO(
            eventId, optionName, winningShares, grossPayout, commissionPaid, netPayout, event.getAccount().getBalance()
        );
    }
}
