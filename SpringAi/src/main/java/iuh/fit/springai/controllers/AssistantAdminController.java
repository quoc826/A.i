package iuh.fit.springai.controllers;
import iuh.fit.springai.services.*;
import iuh.fit.springai.entities.TrackingEvent;
import iuh.fit.springai.repositories.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
@Controller @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AssistantAdminController {
 private final ProductEmbeddingService embeddings;private final TrackingEventRepository events;
 @GetMapping("/admin/research") String page(Model model){
  model.addAttribute("index",embeddings.status());model.addAttribute("embeddingEnabled",embeddings.enabled());
  var counts=new LinkedHashMap<String,Long>();for(var type:TrackingEvent.Type.values())counts.put(type.name(),events.countByEventType(type));
  model.addAttribute("counts",counts);model.addAttribute("events",events.findTop50ByOrderByReceivedAtDesc());return "admin/research";
 }
 @PostMapping("/admin/research/reindex") String reindex(){embeddings.sync();return "redirect:/admin/research";}
}
