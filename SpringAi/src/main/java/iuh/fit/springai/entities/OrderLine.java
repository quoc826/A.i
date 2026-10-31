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
@Table(name="orderlines")
@Getter @Setter
public class OrderLine {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@ManyToOne(optional=false) private Order order;
@ManyToOne(optional=false) private Product product;
@NotNull @Positive private Integer amount;
@NotNull @Positive private BigDecimal purchasePrice;
public BigDecimal getSubtotal(){ return purchasePrice.multiply(BigDecimal.valueOf(amount)); }
}

