package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding,String> {
 List<ProductEmbedding> findByStoreKey(String key);
}
