package org.example.web;

import org.example.model.Article;
import org.example.model.UserAccount;
import org.example.service.ArticleService;
import org.example.service.CheckInService;
import org.example.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
public class HomeController {
    private final ArticleService articleService;
    private final UserAccountService userAccountService;
    private final CheckInService checkInService;

    public HomeController(ArticleService articleService,
                          UserAccountService userAccountService,
                          CheckInService checkInService) {
        this.articleService = articleService;
        this.userAccountService = userAccountService;
        this.checkInService = checkInService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("articles", articleService.findAll());
        if (authentication != null) {
            UserAccount user = currentUser(authentication);
            model.addAttribute("currentUser", user);
            model.addAttribute("checkedInToday", checkInService.hasCheckedInToday(user));
            model.addAttribute("checkInCount", checkInService.totalCount(user));
            model.addAttribute("recentCheckIns", checkInService.recent(user));
        }
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/solutions")
    public String solutions() {
        return "solutions";
    }

    @GetMapping("/news")
    public String news(Model model) {
        model.addAttribute("articles", articleService.findAll());
        return "news";
    }

    @GetMapping("/stock-sim")
    public String stockSim() {
        return "stock-sim";
    }

    @GetMapping("/cases")
    public String cases() {
        return "cases";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("articles", articleService.findAll());
        return "contact";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterForm registerForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            userAccountService.register(registerForm);
        } catch (IllegalArgumentException e) {
            String field = e.getMessage().contains("Email") ? "email" : "username";
            bindingResult.rejectValue(field, "error." + field, e.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/articles/new")
    public String articlePage(Model model) {
        model.addAttribute("articleForm", new ArticleForm());
        return "article-form";
    }

    @PostMapping("/articles")
    public String createArticle(@Valid @ModelAttribute ArticleForm articleForm,
                                BindingResult bindingResult,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "article-form";
        }
        articleService.create(articleForm, file, currentUser(authentication));
        return "redirect:/";
    }

    @GetMapping("/articles/{id}")
    public String articleDetail(@PathVariable Long id, Model model) {
        Article article = articleService.findById(id);
        model.addAttribute("article", article);
        model.addAttribute("comments", articleService.findComments(id));
        model.addAttribute("commentForm", new CommentForm());
        return "article-detail";
    }

    @PostMapping("/articles/{id}/comment")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute CommentForm commentForm,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        Article article = articleService.findById(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("article", article);
            model.addAttribute("comments", articleService.findComments(id));
            return "article-detail";
        }
        articleService.addComment(article, currentUser(authentication), commentForm.getContent());
        return "redirect:/articles/" + id;
    }

    @PostMapping("/checkin")
    public String checkIn(Authentication authentication) {
        checkInService.checkIn(currentUser(authentication));
        return "redirect:/";
    }

    private UserAccount currentUser(Authentication authentication) {
        return userAccountService.getByUsername(authentication.getName());
    }
}
