package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.PasswordChangeForm;
import vn.edu.demo.topicmanagement.service.AuditLogService;
import vn.edu.demo.topicmanagement.service.UserAccountService;

/** Lets any signed-in user change their own password after confirming the current password. */
@Controller
@RequestMapping("/account/password")
public class AccountController {
    private final UserAccountService accountService;
    private final AuditLogService auditLogService;

    public AccountController(UserAccountService accountService, AuditLogService auditLogService) {
        this.accountService = accountService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "account/password";
    }

    @PostMapping
    public String change(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                         BindingResult bindingResult, Principal principal,
                         RedirectAttributes redirect, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getFieldError() == null
                    ? "Thông tin mật khẩu chưa hợp lệ."
                    : bindingResult.getFieldError().getDefaultMessage());
            return "account/password";
        }
        try {
            accountService.changeOwnPassword(principal.getName(), form);
            auditLogService.record(principal.getName(), AuditLogService.PASSWORD_CHANGE,
                    "User", null, "Người dùng tự đổi mật khẩu");
            redirect.addFlashAttribute("message", "Đã đổi mật khẩu thành công.");
            return "redirect:/account/password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "account/password";
        }
    }
}
