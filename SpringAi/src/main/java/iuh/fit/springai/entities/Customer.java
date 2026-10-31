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
@Table(name="customers")
@Getter @Setter
public class Customer {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@NotBlank @Pattern(regexp="\\p{Lu}[^\\s]*(?:\\s+\\p{L}[^\\s]*)+", message="Họ tên phải bắt đầu bằng chữ hoa và có ít nhất 2 từ") private String name;
@NotNull @Past private LocalDate customerSince;
@NotBlank @Email @Pattern(regexp="[^@\\s]+@gmail\\.com", message="Email phải có đuôi @gmail.com") private String email;
@OneToMany(mappedBy="customer") private List<Order> orders = new ArrayList<>();
}

