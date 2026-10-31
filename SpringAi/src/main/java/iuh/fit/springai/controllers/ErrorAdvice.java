package iuh.fit.springai.controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
@ControllerAdvice
public class ErrorAdvice {
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
 String invalid(IllegalArgumentException e,Model model){model.addAttribute("message",e.getMessage());return "failure";}
 @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class) @ResponseStatus(HttpStatus.CONFLICT)
 String conflict(Model model){model.addAttribute("message","Dữ liệu đã thay đổi. Vui lòng tải lại trang.");return "failure";}
}
