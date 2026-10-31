package iuh.fit.springai.controllers;
import iuh.fit.springai.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@RestController @RequiredArgsConstructor @Validated
public class SearchApiController {
 private final ProductSearchService search;private final RequestBudget budget;
 @GetMapping("/api/v1/search")
 public ProductSearchService.Result search(@RequestParam @NotBlank @Size(max=2000) String query,@RequestParam(required=false) @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal maxPrice,@RequestParam(defaultValue="false") boolean inStock,HttpSession session){
  budget.check(session,"search",30);return search.search(query,maxPrice,inStock);
 }
}
