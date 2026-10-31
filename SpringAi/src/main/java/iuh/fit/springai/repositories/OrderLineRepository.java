package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.OrderLine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface OrderLineRepository extends JpaRepository<OrderLine,Long> {  }

