package andy.crypto.pairstrading.bot.pairstrading.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 錯誤處理控制器
 * 提供自定義錯誤頁面
 */
@Slf4j
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    /**
     * 處理所有錯誤
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        Exception exception = (Exception) request.getAttribute("jakarta.servlet.error.exception");
        
        log.error("錯誤處理 - 狀態碼: {}", statusCode);
        if (exception != null) {
            log.error("錯誤詳情", exception);
        }
        
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", "發生錯誤，請聯繫系統管理員");
        
        return "dashboard_view"; // 使用儀表板模板作為錯誤頁面
    }
}
