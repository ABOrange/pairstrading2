package andy.crypto.pairstrading.bot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 主頁控制器
 */
@Controller
@RequestMapping("/")
public class HomeController {

    /**
     * 主頁 - 重定向到儀表板
     */
    @GetMapping
    public String home() {
        return "redirect:/dashboard";
    }
}
