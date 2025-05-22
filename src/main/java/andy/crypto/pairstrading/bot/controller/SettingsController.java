package andy.crypto.pairstrading.bot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import andy.crypto.pairstrading.bot.entity.TradingConfig;
import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.service.ApiConfigService;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 設定頁面控制器
 */
@Controller
@RequestMapping("/settings")
@Slf4j
public class SettingsController {

    @Autowired
    private ApiConfigService apiConfigService;
    
    @Autowired
    private TradingConfigService tradingConfigService;
    
    @Autowired
    private BinanceConfig binanceConfig;
    
    @Autowired
    private BinanceApiService binanceApiService;
    
    /**
     * 顯示API配置頁面
     */
    @GetMapping("/api-config")
    public String showApiConfigPage(Model model) {
        // 獲取當前配置信息
        String apiKey = null;
        String secretKey = null;
        String baseUrl = "https://testnet.binancefuture.com"; // 默認值
        boolean testnetEnabled = true; // 默認值
        
        try {
            if (binanceConfig != null) {
                apiKey = binanceConfig.getApiKey();
                secretKey = binanceConfig.getSecretKey();
                baseUrl = binanceConfig.getBaseUrl();
                testnetEnabled = binanceConfig.isTestnetEnabled();
            }
        } catch (Exception e) {
            log.warn("獲取API配置信息失敗: {}", e.getMessage());
        }
        
        // 判斷是否已配置API金鑰
        boolean apiConfigured = apiKey != null && !apiKey.isEmpty() && secretKey != null && !secretKey.isEmpty();
        
        // 為了安全性，在頁面上只顯示部分API金鑰
        if (apiKey != null && apiKey.length() > 8) {
            apiKey = apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
        }
        
        if (secretKey != null && secretKey.length() > 8) {
            secretKey = secretKey.substring(0, 4) + "..." + secretKey.substring(secretKey.length() - 4);
        }
        
        model.addAttribute("apiKey", apiKey);
        model.addAttribute("secretKey", secretKey);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("testnetEnabled", testnetEnabled);
        model.addAttribute("apiConfigured", apiConfigured);
        model.addAttribute("activeTab", "api-config");
        model.addAttribute("activeMenu", "home");
        return "settings/api-config";
    }
    
    /**
     * 顯示交易配置頁面
     */
    @GetMapping("/trading")
    public String showTradingConfigPage(Model model) {
        Map<String, List<TradingConfig>> configsByCategory = tradingConfigService.getConfigsByCategories();
        model.addAttribute("configsByCategory", configsByCategory);
        model.addAttribute("activeTab", "trading-config");
        
        // 添加簡化的配置數據用於模板
        // 通用設定
        model.addAttribute("tradingEnable", tradingConfigService.isTradingEnabled());
        
        // 交易對設定
        model.addAttribute("asset1", tradingConfigService.getAsset1());
        model.addAttribute("asset2", tradingConfigService.getAsset2());
        
        // 窗口設定
        model.addAttribute("windowSize", tradingConfigService.getWindowSize());
        
        // 閾值設定
        model.addAttribute("entryThreshold", tradingConfigService.getEntryThreshold());
        model.addAttribute("exitThreshold", tradingConfigService.getExitThreshold());
        
        // 倉位設定
        model.addAttribute("positionSize", tradingConfigService.getPositionSize());
        model.addAttribute("amountBased", tradingConfigService.isAmountBased());
        model.addAttribute("leverage", tradingConfigService.getLeverage());
        
        // 控制台設定
        model.addAttribute("consoleChart", tradingConfigService.isConsoleChartEnabled());
        model.addAttribute("consoleSignal", tradingConfigService.isConsoleSignalEnabled());
        
        return "settings/trading-config";  // 確保這與文件名完全匹配
    }
    
    /**
     * 處理API配置表單提交
     */
    @PostMapping("/update-api-config")
    public String updateApiConfig(
            @RequestParam String apiKey,
            @RequestParam String secretKey,
            @RequestParam(required = false, defaultValue = "false") boolean testnetEnabled,
            RedirectAttributes redirectAttributes) {
        
        // 驗證輸入
        if (apiKey == null || apiKey.trim().isEmpty() || secretKey == null || secretKey.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "API金鑰和秘鑰不能為空");
            return "redirect:/settings/api-config";
        }
        
        // 保存到資料庫
        apiConfigService.saveOrUpdateConfig(ApiConfigService.BINANCE_API_KEY, apiKey, "幣安API金鑰");
        apiConfigService.saveOrUpdateConfig(ApiConfigService.BINANCE_SECRET_KEY, secretKey, "幣安API秘鑰");
        apiConfigService.saveOrUpdateConfig(ApiConfigService.BINANCE_TESTNET_ENABLED, String.valueOf(testnetEnabled), "幣安測試網絡啟用狀態");
        
        // 更新當前配置
        binanceConfig.setApiKey(apiKey);
        binanceConfig.setSecretKey(secretKey);
        binanceConfig.setTestnetEnabled(testnetEnabled); // 這會自動更新 baseUrl
        
        log.info("幣安API金鑰和測試網絡設置已更新，測試網絡啟用狀態: {}", testnetEnabled);
        
        redirectAttributes.addFlashAttribute("success", "API金鑰、秘鑰和網絡設置已成功更新");
        return "redirect:/settings/api-config";
    }
    
    /**
     * 測試API連接
     */
    @PostMapping("/test-api-connection")
    public String testApiConnection(RedirectAttributes redirectAttributes) {
        // 獲取當前配置
        String apiKey = null;
        String secretKey = null;
        
        try {
            if (binanceConfig != null) {
                apiKey = binanceConfig.getApiKey();
                secretKey = binanceConfig.getSecretKey();
            }
        } catch (Exception e) {
            log.warn("獲取API配置信息失敗: {}", e.getMessage());
        }
        
        // 檢查是否已配置API金鑰
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "請先設定API金鑰和秘鑰");
            return "redirect:/settings/api-config";
        }
        
        try {
            // 測試API連接 - 嘗試獲取系統狀態或帳戶餘額
            Map<String, java.math.BigDecimal> balances = binanceApiService.getAccountBalance();
            
            // 如果沒有拋出異常，則連接成功
            redirectAttributes.addFlashAttribute("success", "API連接測試成功，成功獲取帳戶餘額！");
            
            // 記錄API金鑰前後幾位作為參考
            String maskedApiKey = apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
            log.info("API連接測試成功: apiKey={}", maskedApiKey);
            
        } catch (Exception e) {
            log.error("API連接測試失敗", e);
            redirectAttributes.addFlashAttribute("error", "API連接測試失敗: " + e.getMessage());
        }
        
        return "redirect:/settings/api-config";
    }
}
