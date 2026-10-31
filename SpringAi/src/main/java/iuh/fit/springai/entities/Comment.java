package iuh.fit.springai.entities;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="comments")
@Getter @Setter
public class Comment {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@NotBlank @Size(max=1000) private String text;
@ManyToOne(optional=false) private Product product;
@ManyToOne(optional=false) private Customer customer;
private LocalDate date = LocalDate.now();
}

