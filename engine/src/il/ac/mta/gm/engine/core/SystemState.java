package il.ac.mta.gm.engine.core;

import il.ac.mta.gm.engine.model.domain.GMEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemState {
    private List<GMEvent> events;

    public SystemState() {
        this.events = new ArrayList<>();
    }

    public void setEvents(List<GMEvent> events) {
        this.events = new ArrayList<>(events);
    }

    public List<GMEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
    
    public GMEvent getEventById(int eventId) {
        for (GMEvent event : events) {
            if (event.getId() == eventId) {
                return event;
            }
        }
        return null;
    }
}
