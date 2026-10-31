package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.Product;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,Long> { @EntityGraph(attributePaths={"category","comments"})
List<Product> findByActiveTrueOrderByNameAsc();
@EntityGraph(attributePaths={"category","comments","comments.customer"})
Optional<Product> findByIdAndActiveTrue(Long id);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Product p where p.id = :id") Optional<Product> lockById(@Param("id") Long id); }

