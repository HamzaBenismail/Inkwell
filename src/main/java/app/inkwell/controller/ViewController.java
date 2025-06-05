package app.inkwell.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class ViewController {

    @GetMapping("/SignIn")
    public String showLoginPage() {
        return "SignIn";
    }

    @GetMapping("/SignUp")
    public String showRegistrationPage() {
        return "SignUp";
    }
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/index")
	public String Index(Model model) {
		model.addAttribute("StoryPage", "StoryPage");
		return "index";
	}
	
	@GetMapping("/target")
    public String targetPage() {
        return "Main"; // Refers to the "target.html" Thymeleaf template
    }
	
	@GetMapping("/reading")
    public String reading() {
        return "Reading"; // Refers to the "target.html" Thymeleaf template
    }
	

		
	@GetMapping("/Files")
	public String Files() {
		return "Files";
	}
	
	@GetMapping("/statistics")
	public String Statistics() {
		return "statistics";
	}
}

