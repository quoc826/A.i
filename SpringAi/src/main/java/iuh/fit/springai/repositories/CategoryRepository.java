package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.Category;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CategoryRepository extends JpaRepository<Category,Long> { List<Category> findByActiveTrueOrderByNameAsc(); }

