package app.inkwell.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.PrintWriter;

@Controller
public class PlaceholderController {

    @GetMapping(value = "/placeholder.svg", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public void generatePlaceholderSvg(
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "150") int height,
            @RequestParam(defaultValue = "cccccc") String bgColor,
            @RequestParam(defaultValue = "666666") String textColor,
            @RequestParam(defaultValue = "Placeholder") String text,
            HttpServletResponse response) throws IOException {
        
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">" +
                "<rect width=\"%d\" height=\"%d\" fill=\"#%s\"/>" +
                "<text x=\"50%%\" y=\"50%%\" font-family=\"Arial\" font-size=\"24\" text-anchor=\"middle\" " +
                "dominant-baseline=\"middle\" fill=\"#%s\">%s</text>" +
                "</svg>",
                width, height, width, height, width, height, bgColor, textColor, text
            ));
        }
    }
}

