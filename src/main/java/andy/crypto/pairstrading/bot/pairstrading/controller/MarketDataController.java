package andy.crypto.pairstrading.bot.pairstrading.controller;

import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 市場數據 API 控制器
 */
@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    @Autowired
    private BinanceApiService binanceApiService;
    
    @Value("${trading.pair.asset1:BNBUSDT}")
    private String asset1;

    @Value("${trading.pair.asset2:SOLUSDT}")
    private String asset2;
    
    /**
     * 獲取資產價格
     */
    @GetMapping("/prices")
    public Map<String, Object> getPrices() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 嘗試獲取價格
            BigDecimal asset1Price = binanceApiService.getLatestPrice(asset1);
            BigDecimal asset2Price = binanceApiService.getLatestPrice(asset2);
            
            Map<String, Object> prices = new HashMap<>();
            
            // 確保返回的 symbol 是乾淨的沒有額外空格
            prices.put(asset1.trim(), asset1Price);
            prices.put(asset2.trim(), asset2Price);
            
            // 添加日誌輸出以協助調試
            System.out.println("API 返回價格數據 - " + asset1.trim() + ": " + asset1Price + ", " + asset2.trim() + ": " + asset2Price);
            
            result.put("prices", prices);
            result.put("status", "success");
        } catch (Exception e) {
            System.err.println("獲取價格失敗: " + e.getMessage());
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", "獲取價格失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 獲取持倉數據
     */
    @GetMapping("/positions")
    public Map<String, Object> getPositions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<PositionInfo> positions = binanceApiService.getPositionInfo(null);
            result.put("positions", positions);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取持倉失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 獲取帳戶餘額
     */
    @GetMapping("/balance")
    public Map<String, Object> getBalance() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, BigDecimal> balances = binanceApiService.getAccountBalance();
            result.put("balances", balances);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取餘額失敗: " + e.getMessage());
        }
        
        return result;
    }
}
