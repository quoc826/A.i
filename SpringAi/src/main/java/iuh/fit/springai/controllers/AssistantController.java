package iuh.fit.springai.controllers;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AssistantController {
 @GetMapping("/assistant") String page(){return "assistant";}
}
