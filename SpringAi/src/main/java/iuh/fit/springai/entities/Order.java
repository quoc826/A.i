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
@Table(name="orders")
@Getter @Setter
public class Order {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@NotNull private LocalDate date;
@ManyToOne(optional=false) private Customer customer;
@OneToMany(mappedBy="order", cascade=CascadeType.ALL) private List<OrderLine> orderLines = new ArrayList<>();
private String status = "PLACED";
public BigDecimal getTotal(){return orderLines.stream().map(OrderLine::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
}

