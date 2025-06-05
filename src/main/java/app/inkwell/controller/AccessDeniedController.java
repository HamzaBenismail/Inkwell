package app.inkwell.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessDeniedController {

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("message", "You need to be a writer to access this page.");
        model.addAttribute("actionText", "Become a Writer");
        model.addAttribute("actionUrl", "/Settings?tab=writers");
        return "error/403";
    }
}
