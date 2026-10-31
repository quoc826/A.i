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
@Table(name="products")
@Getter @Setter
public class Product {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@NotBlank @Pattern(regexp="\\p{Lu}.{1,}", message="Tên phải bắt đầu bằng chữ hoa và có ít nhất 2 ký tự") private String name;
@NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=12,fraction=2) private BigDecimal price;
public interface Input {}
@NotNull @PositiveOrZero @Positive(groups=Input.class) private Integer quantity;
@ManyToOne(optional=false) @NotNull private Category category;
private String image;
@Size(max=100) private String brand;
@Size(max=4000) @Column(length=4000) private String description;
@Size(max=2000) @Column(length=2000) private String specifications;
@OneToMany(mappedBy="product", cascade=CascadeType.ALL) private List<Comment> comments = new ArrayList<>();
@OneToMany(mappedBy="product") private List<OrderLine> orderLines = new ArrayList<>();
public boolean isInStock(){ return active && quantity != null && quantity > 0; }
}

