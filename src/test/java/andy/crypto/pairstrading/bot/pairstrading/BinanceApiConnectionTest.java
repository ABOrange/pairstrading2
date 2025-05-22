package andy.crypto.pairstrading.bot.pairstrading;

import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幣安API連接測試
 * 基本連接測試，只測試最核心的API功能
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class BinanceApiConnectionTest {

    @Autowired
    private BinanceApiService binanceApiService;

    /**
     * 測試幣安API連接
     * 獲取BNBUSDT當前價格作為基本連接測試
     */
    @Test
    @DisplayName("測試幣安API連接 - 獲取BNBUSDT價格")
    public void testBinanceConnection() {
        try {
            // 獲取BNBUSDT的最新價格
            BigDecimal btcPrice = binanceApiService.getLatestPrice("BNBUSDT");
            
            // 輸出測試結果
            log.info("幣安API連接測試成功");
            log.info("BNBUSDT 當前價格: {}", btcPrice);
            
            // 驗證價格不為空且大於0
            assertNotNull(btcPrice, "BNBUSDT價格不應為空");
            assertTrue(btcPrice.compareTo(BigDecimal.ZERO) > 0, "BNBUSDT價格應該大於0");
        } catch (Exception e) {
            log.error("幣安API連接測試失敗", e);
            log.error("請檢查您的API密鑰配置和網絡連接");
            fail("API連接測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試獲取帳戶餘額
     * 驗證API密鑰的有效性
     */
    @Test
    @DisplayName("測試幣安API密鑰 - 獲取帳戶餘額")
    public void testApiKeyAuth() {
        try {
            // 獲取帳戶餘額 (需要有效的API密鑰)
            Map<String, BigDecimal> balances = binanceApiService.getAccountBalance();
            
            // 輸出測試結果
            log.info("幣安API密鑰驗證成功");
            log.info("帳戶餘額: {}", balances);
            
            // 驗證餘額不為空
            assertNotNull(balances, "帳戶餘額不應為空");
        } catch (Exception e) {
            log.error("幣安API密鑰驗證失敗", e);
            log.error("請確認您的API密鑰配置正確且具有適當的權限");
            fail("API密鑰驗證失敗: " + e.getMessage());
        }
    }
}
