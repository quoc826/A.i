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
@Table(name="categories")
@Getter @Setter
public class Category {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
private boolean active = true;
@Version private Long version;
@NotBlank @Pattern(regexp="\\p{Lu}.{1,}", message="Tên phải bắt đầu bằng chữ hoa và có ít nhất 2 ký tự") private String name;
}

