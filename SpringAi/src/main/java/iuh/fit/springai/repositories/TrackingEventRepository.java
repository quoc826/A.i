package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TrackingEventRepository extends JpaRepository<TrackingEvent,String> {
 long countByEventType(TrackingEvent.Type type);
 List<TrackingEvent> findTop50ByOrderByReceivedAtDesc();
}
