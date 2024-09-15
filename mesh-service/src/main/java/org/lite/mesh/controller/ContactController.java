package org.lite.mesh.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mesh")
public class ContactController {

    @GetMapping(value = "/contact", name = "contact")
    public String about(Model model) {
        model.addAttribute("module", "contact");
        return "contact";
    }
}
