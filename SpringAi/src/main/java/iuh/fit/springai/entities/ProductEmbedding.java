package iuh.fit.springai.entities;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="product_embeddings") @Getter @Setter
public class ProductEmbedding {
 @Id @Column(length=36) private String id;
 @Column(nullable=false) private Long productId;
 @Column(nullable=false,length=200) private String storeKey;
 @Column(nullable=false,length=64) private String contentHash;
 private Instant syncedAt;
}
