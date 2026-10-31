package iuh.fit.springai.repositories;
import iuh.fit.springai.entities.Comment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CommentRepository extends JpaRepository<Comment,Long> {  }

