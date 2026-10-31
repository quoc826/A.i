package iuh.fit.springai.controllers;
import iuh.fit.springai.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController @RequiredArgsConstructor
public class TrackingController {
 private final TrackingService tracking;private final RequestBudget budget;
 @PostMapping("/api/v1/tracking/events") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
 public void event(@Valid @RequestBody TrackingService.EventInput input,Authentication auth,HttpSession session){budget.check(session,"tracking",120);tracking.record(input,auth,session);}
}
