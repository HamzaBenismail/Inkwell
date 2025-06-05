package app.inkwell.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("error", "Page not found");
                model.addAttribute("message", "The requested page does not exist.");
                model.addAttribute("statusCode", statusCode);
                return "error/404";
            }
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("error", "Internal Server Error");
                model.addAttribute("message", "Something went wrong on our end. Please try again later.");
                model.addAttribute("statusCode", statusCode);
                return "error/500";
            }
            else if(statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("error", "Access Denied");
                model.addAttribute("message", "You don't have permission to access this resource.");
                model.addAttribute("statusCode", statusCode);
                return "error/403";
            }
        }
        
        model.addAttribute("error", "Error");
        model.addAttribute("message", errorMessage != null ? errorMessage : "An unexpected error occurred.");
        model.addAttribute("statusCode", status != null ? status : "Unknown");
        return "error/general";
    }
}

