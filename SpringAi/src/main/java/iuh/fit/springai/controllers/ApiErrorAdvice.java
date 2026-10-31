package iuh.fit.springai.controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
@RestControllerAdvice(assignableTypes={AssistantApiController.class,SearchApiController.class,TrackingController.class})
@org.springframework.core.annotation.Order(0)
public class ApiErrorAdvice {
 @ExceptionHandler({MethodArgumentNotValidException.class,HandlerMethodValidationException.class,ConstraintViolationException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
 ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Dữ liệu không hợp lệ. Câu hỏi tối đa 2000 ký tự; giá phải lớn hơn 0."));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> argument(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Không thể thực hiện yêu cầu":e.getReason()));}
}
