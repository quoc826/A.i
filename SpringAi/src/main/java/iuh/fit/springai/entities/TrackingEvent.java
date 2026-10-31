package iuh.fit.springai.entities;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="tracking_events",indexes={@Index(name="idx_tracking_type_time",columnList="eventType,receivedAt"),@Index(name="idx_tracking_user",columnList="userId")}) @Getter @Setter
public class TrackingEvent {
 public enum Type {PAGE_VIEW,PRODUCT_VIEW,SEARCH,ADD_TO_CART}
 @Id @Column(length=36) private String id;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Type eventType;
 private Long userId;
 @Column(nullable=false,length=36) private String anonymousId;
 private Long productId;
 @Column(length=2000) private String keyword;
 private Integer resultCount;
 private Integer quantity;
 private BigDecimal price;
 @Column(length=500) private String path;
 @Column(length=200) private String title;
 private Instant clientTimestamp;
 @Column(nullable=false) private Instant receivedAt;
}
