package andy.crypto.pairstrading.bot.controller;

import andy.crypto.pairstrading.bot.pairstrading.exception.BinanceApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;

/**
 * 全局錯誤處理控制器
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 處理幣安API異常
     */
    @ExceptionHandler(BinanceApiException.class)
    public String handleBinanceApiException(BinanceApiException ex, Model model) {
        log.error("幣安API異常: {}", ex.getMessage());
        
        // 確保模型中包含基本變數，避免空指針異常
        ensureModelContainsDefaults(model);
        
        String errorMessage = ex.getMessage();
        // 檢查是否是API金鑰相關的錯誤
        if (errorMessage != null && (errorMessage.contains("API金鑰") || errorMessage.contains("未配置"))) {
            model.addAttribute("errorTitle", "API設定錯誤");
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("redirectUrl", "/settings/api-config");
            model.addAttribute("redirectText", "前往API設定頁面");
            return "error/api-error";
        }
        
        // 一般API錯誤
        model.addAttribute("errorTitle", "幣安API異常");
        model.addAttribute("errorMessage", errorMessage);
        return "error/api-error";
    }
    
    /**
     * 處理一般異常
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        log.error("系統異常: {}", ex.getMessage(), ex);
        
        // 確保模型中包含基本變數，避免空指針異常
        ensureModelContainsDefaults(model);
        
        // 添加錯誤信息
        model.addAttribute("errorTitle", "系統錯誤");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("error", "發生錯誤: " + ex.getMessage());
        
        // 檢查是否是API相關的錯誤
        String message = ex.getMessage();
        if (message != null && (message.contains("API") || message.contains("幣安"))) {
            return "error/api-error";
        }
        
        return "error/general-error";
    }
    
    /**
     * 確保模型中包含基本變數，避免空指針異常
     */
    private void ensureModelContainsDefaults(Model model) {
        // 確保模型中包含基本變數，避免空指針異常
        if (!model.containsAttribute("positions")) {
            model.addAttribute("positions", new ArrayList<>());
        }
        
        // 可以根據需要添加其他必要的空值或默認值
        if (!model.containsAttribute("asset1")) {
            model.addAttribute("asset1", "BNBUSDT");
        }
        
        if (!model.containsAttribute("asset2")) {
            model.addAttribute("asset2", "SOLUSDT");
        }
        
        if (!model.containsAttribute("asset1Price")) {
            model.addAttribute("asset1Price", "0.00");
        }
        
        if (!model.containsAttribute("asset2Price")) {
            model.addAttribute("asset2Price", "0.00");
        }
        
        if (!model.containsAttribute("zScoreReport")) {
            model.addAttribute("zScoreReport", "數據載入失敗");
        }
    }
}
