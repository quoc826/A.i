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
@Table(name="users")
@Getter @Setter
public class User {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@Column(unique=true,nullable=false) private String username;
@Column(nullable=false) private String password;
private String role = "CUSTOMER";
@OneToOne private Customer customer;
}

