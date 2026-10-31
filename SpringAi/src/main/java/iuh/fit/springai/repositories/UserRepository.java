package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface UserRepository extends JpaRepository<User,Long> { @EntityGraph(attributePaths={"customer"}) Optional<User> findByUsername(String username);
Optional<User> findByCustomerId(Long customerId); }

