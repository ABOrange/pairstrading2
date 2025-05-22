package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import andy.crypto.pairstrading.bot.pairstrading.service.LogService;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import andy.crypto.pairstrading.bot.pairstrading.config.BinanceConfig;
import andy.crypto.pairstrading.bot.service.TradingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * 儀表板控制器
 * 用於提供web頁面以顯示交易機器人的運行狀態和圖表
 * 注意：API 相關功能已移至專用控制器，包括：
 * - ChartApiController: 處理圖表數據 API
 * - LogApiController: 處理日誌相關 API
 * - TradingConfigApiController: 處理交易配置 API
 */
@Slf4j
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private PairsTradingService pairsTradingService;
    
    @Autowired
    private BinanceApiService binanceApiService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private BinanceConfig binanceConfig;
    
    @Autowired
    private TradingConfigService tradingConfigService;
    
    // 預設值僅作為備用，實際使用資料庫中的值
    @Value("${trading.pair.asset1:BNBUSDT}")
    private String asset1Default;

    @Value("${trading.pair.asset2:SOLUSDT}")
    private String asset2Default;
    
    /**
     * 儀表板首頁 - 重定向到總覽頁面
     */
    @GetMapping("")
    public String dashboard() {
        return "redirect:/dashboard/overview";
    }
    
    /**
     * 儀表板首頁 - 顯式根路徑映射
     */
    @GetMapping("/")
    public String dashboardRoot() {
        return "redirect:/dashboard/overview";
    }
    
    /**
     * 設定頁面 - 重定向到API設定頁面
     */
    @GetMapping("/settings")
    public String settings() {
        return "redirect:/settings/api-config";
    }
    
    /**
     * 儀表板索引頁
     */
    @GetMapping("/index")
    public String dashboardIndex() {
        return "dashboard/index";
    }
    
    /**
     * 總覽頁面
     */
    @GetMapping("/overview")
    public String overview(Model model) {
        try {
            // 從資料庫獲取資產對設定
            String asset1 = tradingConfigService.getAsset1();
            String asset2 = tradingConfigService.getAsset2();
            
            // 如果資料庫中沒有設定，則使用預設值
            if (asset1 == null || asset1.isEmpty()) {
                asset1 = asset1Default;
                log.warn("從資料庫獲取資產1失敗，使用預設值: {}", asset1Default);
            }
            
            if (asset2 == null || asset2.isEmpty()) {
                asset2 = asset2Default;
                log.warn("從資料庫獲取資產2失敗，使用預設值: {}", asset2Default);
            }
            
            // 獲取API設定狀態
            String apiKey = null;
            try {
                if (binanceConfig != null) {
                    apiKey = binanceConfig.getApiKey();
                }
            } catch (Exception e) {
                log.warn("獲取API設定狀態失敗: {}", e.getMessage());
            }
            model.addAttribute("apiKey", apiKey);
            
            // 獲取資產最新價格
            try {
                BigDecimal asset1Price = binanceApiService.getLatestPrice(asset1);
                BigDecimal asset2Price = binanceApiService.getLatestPrice(asset2);
                model.addAttribute("asset1Price", asset1Price);
                model.addAttribute("asset2Price", asset2Price);
            } catch (Exception e) {
                log.warn("獲取最新價格失敗：{}", e.getMessage());
                // 如果API未配置或API調用失敗，顯示預設值
                model.addAttribute("asset1Price", BigDecimal.ZERO);
                model.addAttribute("asset2Price", BigDecimal.ZERO);
            }
            
            // 獲取持倉信息
            List<PositionInfo> positions = new java.util.ArrayList<>();
            try {
                if (apiKey != null && !apiKey.isEmpty()) {
                    positions = binanceApiService.getPositionInfo(null);
                }
            } catch (Exception e) {
                log.warn("獲取持倉信息失敗：{}", e.getMessage());
            }
            
            // 確保positions不為null
            if (positions == null) {
                positions = new java.util.ArrayList<>();
            }
            
            // 獲取當前窗口大小
            int windowSize = tradingConfigService.getTradingConfigBean().getWindowSize();
            
            // 傳遞數據到模板
            model.addAttribute("asset1", asset1);
            model.addAttribute("asset2", asset2);
            model.addAttribute("positions", positions);
            model.addAttribute("activeTab", "overview");
            model.addAttribute("windowSize", windowSize);
            
            // 獲取Z分數圖表數據
            try {
                model.addAttribute("zScoreReport", pairsTradingService.getSignalReport());
            } catch (Exception e) {
                log.warn("獲取信號報告失敗：{}", e.getMessage());
                model.addAttribute("zScoreReport", "尚無交易信號數據");
            }
            
            return "dashboard/overview";
        } catch (Exception e) {
            log.error("加載總覽頁面數據失敗", e);
            model.addAttribute("error", "加載數據失敗: " + e.getMessage());
            // 確保即使出錯也有空的positions列表
            model.addAttribute("positions", new java.util.ArrayList<>());
            model.addAttribute("activeTab", "overview");
            return "dashboard/overview";
        }
    }
    
    /**
     * 交易圖表頁面
     */
    @GetMapping("/charts")
    public String charts(Model model) {
        model.addAttribute("activeTab", "charts");
        return "dashboard/charts";
    }
    
    /**
     * 系統日誌頁面
     */
    @GetMapping("/system-logs")
    public String systemLogs(Model model) {
        model.addAttribute("activeTab", "system-logs");
        return "dashboard/system-logs";
    }
    
    /**
     * 交易操作日誌頁面
     */
    @GetMapping("/trade-logs")
    public String tradeLogs(Model model) {
        model.addAttribute("activeTab", "trade-logs");
        return "dashboard/trade-logs";
    }
    
    /**
     * 歷史倉位記錄頁面
     */
    @GetMapping("/position-history")
    public String positionHistory(Model model) {
        try {
            // 從資料庫獲取資產對設定
            String asset1 = tradingConfigService.getAsset1();
            String asset2 = tradingConfigService.getAsset2();
            
            // 如果資料庫中沒有設定，則使用預設值
            if (asset1 == null || asset1.isEmpty()) {
                asset1 = asset1Default;
                log.warn("從資料庫獲取資產1失敗，使用預設值: {}", asset1Default);
            }
            
            if (asset2 == null || asset2.isEmpty()) {
                asset2 = asset2Default;
                log.warn("從資料庫獲取資產2失敗，使用預設值: {}", asset2Default);
            }
            
            model.addAttribute("activeTab", "position-history");
            model.addAttribute("asset1", asset1);
            model.addAttribute("asset2", asset2);
            
            // 獲取當前交易對的最新價格，用於顯示在頁面上
            try {
                BigDecimal asset1Price = binanceApiService.getLatestPrice(asset1);
                BigDecimal asset2Price = binanceApiService.getLatestPrice(asset2);
                model.addAttribute("asset1Price", asset1Price);
                model.addAttribute("asset2Price", asset2Price);
            } catch (Exception e) {
                log.warn("無法獲取最新價格: " + e.getMessage());
                // 如果獲取價格失敗，使用空值
                model.addAttribute("asset1Price", null);
                model.addAttribute("asset2Price", null);
            }
            
            return "dashboard/position-history";
        } catch (Exception e) {
            log.error("載入歷史倉位頁面失敗", e);
            model.addAttribute("error", "載入頁面數據失敗: " + e.getMessage());
            model.addAttribute("activeTab", "position-history");
            return "dashboard/position-history";
        }
    }
}