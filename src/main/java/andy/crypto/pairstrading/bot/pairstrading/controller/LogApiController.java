package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日誌 API 控制器
 * 用於提供系統日誌和交易日誌相關的 API 端點
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogApiController {

    @Autowired
    private LogService logService;
    
    /**
     * 獲取系統日誌
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemLogs(@RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> logs = logService.getLatestLogs(limit);
            result.put("logs", logs);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取系統日誌失敗", e);
            result.put("status", "error");
            result.put("message", "獲取系統日誌失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 獲取交易操作日誌
     */
    @GetMapping("/trade")
    public Map<String, Object> getTradeLogs(@RequestParam(defaultValue = "50") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> logs = logService.getLatestTradeLogs(limit);
            result.put("logs", logs);
            result.put("status", "success");
        } catch (Exception e) {
            log.error("獲取交易日誌失敗", e);
            result.put("status", "error");
            result.put("message", "獲取交易日誌失敗: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 清除日誌
     */
    @GetMapping("/clear")
    public Map<String, Object> clearLogs() {
        Map<String, Object> result = new HashMap<>();
        try {
            logService.clearLogs();
            logService.addLog("日誌已清除", "INFO");
            result.put("message", "日誌已清除");
            result.put("status", "success");
        } catch (Exception e) {
            log.error("清除日誌失敗", e);
            result.put("status", "error");
            result.put("message", "清除日誌失敗: " + e.getMessage());
        }
        return result;
    }
}