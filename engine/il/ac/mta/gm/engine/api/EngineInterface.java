package il.ac.mta.gm.engine.api;

import il.ac.mta.gm.dto.EventDisplayDTO;
import il.ac.mta.gm.dto.EventStatusDTO;
import il.ac.mta.gm.dto.TradeResultDTO;
import il.ac.mta.gm.dto.CloseEventResultDTO;
import java.util.List;

public interface EngineInterface {
    
    // Phase B operations
    List<EventDisplayDTO> getEvents();
    EventStatusDTO getEventStatus(int eventId);
    
    // Phase C
    void loadSystemData(String filePath);
    
    // Phase D
    TradeResultDTO buyShares(int eventId, int optionIndex, int quantity);
    
    // Phase E
    CloseEventResultDTO closeEvent(int eventId, int winningOptionIndex);
}
