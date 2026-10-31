package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.Order;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface OrderRepository extends JpaRepository<Order,Long> { @EntityGraph(attributePaths={"customer","orderLines","orderLines.product"})
List<Order> findByActiveTrueOrderByIdDesc();
@EntityGraph(attributePaths={"customer","orderLines","orderLines.product"})
List<Order> findByCustomerIdAndActiveTrueOrderByIdDesc(Long customerId);
@EntityGraph(attributePaths={"customer","orderLines","orderLines.product"})
Optional<Order> findByIdAndActiveTrue(Long id); }

