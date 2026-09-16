package blog.hethong_quanlydetai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String trangchu(Model model) {
        model.addAttribute("message", "Quản lý đề tài sinh viên");
        return "index";
    }
}
