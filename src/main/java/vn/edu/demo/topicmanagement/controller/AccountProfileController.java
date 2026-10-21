package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.demo.topicmanagement.service.UserAccountService;

/** Shows the signed-in user's own profile. */
@Controller
@RequestMapping("/account/profile")
public class AccountProfileController {
    private final UserAccountService accountService;

    public AccountProfileController(UserAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String page(Principal principal, Model model) {
        model.addAttribute("profile", accountService.getOwnProfile(principal.getName()));
        return "account/profile";
    }
}
