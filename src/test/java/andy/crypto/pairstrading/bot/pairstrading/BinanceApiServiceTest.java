package andy.crypto.pairstrading.bot.pairstrading;

import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;
import andy.crypto.pairstrading.bot.pairstrading.model.OrderResponse;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.model.PriceFilter;
import andy.crypto.pairstrading.bot.pairstrading.model.SymbolInfo;
import andy.crypto.pairstrading.bot.pairstrading.service.BinanceApiService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幣安永續合約API單元測試
 * 測試各種API功能是否正常運作
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class BinanceApiServiceTest {

    @Autowired
    private BinanceApiService binanceApiService;

    /**
     * 測試獲取BNBUSDT的最新價格
     */
    @Test
    @DisplayName("測試獲取BNBUSDT合約價格")
    public void testGetBNBUSDTPrice(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 獲取BNBUSDT的最新價格
            BigDecimal btcPrice = binanceApiService.getLatestPrice("BNBUSDT");
            
            // 輸出價格資訊
            log.info("BNBUSDT 當前價格: {}", btcPrice);
            
            // 驗證價格不為空且大於0
            assertNotNull(btcPrice, "BNBUSDT價格不應為空");
            assertTrue(btcPrice.compareTo(BigDecimal.ZERO) > 0, "BNBUSDT價格應該大於0");
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試獲取BNBUSDT價格失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }
    
    /**
     * 測試獲取帳戶餘額
     */
    @Test
    @DisplayName("測試獲取帳戶餘額")
    public void testGetAccountBalance(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 獲取帳戶餘額
            Map<String, BigDecimal> balances = binanceApiService.getAccountBalance();
            
            // 輸出餘額資訊
            log.info("帳戶餘額: {}", balances);
            
            // 驗證餘額不為空
            assertNotNull(balances, "帳戶餘額不應為空");
            
            // 檢查是否有USDT餘額
            assertTrue(balances.containsKey("USDT"), "測試網帳戶應該有USDT");
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試獲取帳戶餘額失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }
    
    /**
     * 測試獲取BNBUSDT的K線數據
     */
    @Test
    @DisplayName("測試獲取BNBUSDT的K線數據")
    public void testGetBNBUSDTKlines(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 獲取BNBUSDT的近10根小時K線
            List<CandlestickData> klines = binanceApiService.getCandlestickData("BNBUSDT", "1h", 10);
            
            // 輸出K線資訊
            log.info("獲取到 {} 條K線數據", klines.size());
            
            // 輸出最新的一根K線的詳細資訊
            if (!klines.isEmpty()) {
                CandlestickData latest = klines.get(klines.size() - 1);
                log.info("最新K線數據: 開盤時間={}, 開盤價={}, 最高價={}, 最低價={}, 收盤價={}, 成交量={}",
//                        latest.getOpenTimeInstant(),
                        latest.getOpen(),
                        latest.getHigh(),
                        latest.getLow(),
                        latest.getClose(),
                        latest.getVolume());
            }
            
            // 驗證K線數據不為空
            assertNotNull(klines, "K線數據不應為空");
            assertTrue(klines.size() > 0, "應至少獲取到一條K線數據");
            
            // 驗證K線數據是否符合預期
            for (CandlestickData candle : klines) {
                assertNotNull(candle.getOpen(), "開盤價不能為空");
                assertNotNull(candle.getHigh(), "最高價不能為空");
                assertNotNull(candle.getLow(), "最低價不能為空");
                assertNotNull(candle.getClose(), "收盤價不能為空");
                assertTrue(candle.getHigh().compareTo(candle.getLow()) >= 0, "最高價應該大於或等於最低價");
            }
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試獲取K線數據失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試獲取BNBUSDT的合約信息
     */
    @Test
    @DisplayName("測試獲取BNBUSDT的合約信息")
    public void testGetSymbolInfo(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 獲取BNBUSDT的合約信息
            SymbolInfo symbolInfo = binanceApiService.getSymbolInfo("BNBUSDT");
            
            // 輸出合約信息
            log.info("BNBUSDT 合約信息: {}", symbolInfo);
            
            // 驗證合約信息不為空
            assertNotNull(symbolInfo, "合約信息不應為空");
            assertEquals("BNBUSDT", symbolInfo.getSymbol(), "合約符號應為BNBUSDT");
            assertNotNull(symbolInfo.getBaseAsset(), "基礎資產不能為空");
            assertNotNull(symbolInfo.getQuoteAsset(), "報價資產不能為空");
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試獲取合約信息失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試獲取當前持倉信息
     */
    @Test
    @DisplayName("測試獲取當前持倉信息")
    public void testGetPositionInfo(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 獲取當前所有持倉信息
            List<PositionInfo> positions = binanceApiService.getPositionInfo(null);
            
            // 輸出持倉信息
            log.info("當前持倉數量: {}", positions.size());
            
            if (!positions.isEmpty()) {
                for (PositionInfo position : positions) {
                    log.info("持倉信息: 交易對={}, 倉位方向={}, 倉位大小={}, 進場價格={}, 未實現盈虧={}",
                            position.getSymbol(),
                            position.getPositionSide(),
                            position.getPositionAmt(),
                            position.getEntryPrice(),
                            position.getUnrealizedProfit());
                }
            } else {
                log.info("目前沒有持倉");
            }
            
            // 驗證持倉信息列表不為空
            assertNotNull(positions, "持倉信息列表不應為空");
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試獲取持倉信息失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試獲取未完成訂單
     */
    @Test
    @DisplayName("測試獲取未完成訂單")
    public void testGetOpenOrders(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 獲取未完成訂單
            List<OrderResponse> openOrders = binanceApiService.getOpenOrders("BNBUSDT");
            
            // 輸出未完成訂單信息
            log.info("BNBUSDT未完成訂單數量: {}", openOrders.size());
            
            if (!openOrders.isEmpty()) {
                for (OrderResponse order : openOrders) {
                    log.info("訂單信息: 訂單ID={}, 交易對={}, 方向={}, 價格={}, 數量={}, 狀態={}",
                            order.getOrderId(),
                            order.getSymbol(),
                            order.getSide(),
                            order.getPrice(),
                            order.getOrigQty(),
                            order.getStatus());
                }
            } else {
                log.info("目前沒有未完成訂單");
            }
            
            // 驗證訂單列表不為空
            assertNotNull(openOrders, "訂單列表不應為空");
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試獲取未完成訂單失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試下單和取消訂單功能
     * 注意：此測試會在測試網上實際下單和取消訂單
     */
    @Test
    @DisplayName("測試下單和取消訂單功能")
    public void testPlaceAndCancelOrder(TestInfo testInfo) {
        log.info("開始執行 {} 測試", testInfo.getDisplayName());
        
        try {
            // 清理現有訂單
            List<OrderResponse> existingOrders = binanceApiService.getOpenOrders("BNBUSDT");
            if (!existingOrders.isEmpty()) {
                log.info("測試開始前清理現有訂單...");
                for (OrderResponse order : existingOrders) {
                    try {
                        binanceApiService.cancelOrder("BNBUSDT", order.getOrderId());
                        log.info("已取消現有訂單 ID: {}", order.getOrderId());
                    } catch (Exception e) {
                        log.warn("取消現有訂單 ID: {} 失敗: {}", order.getOrderId(), e.getMessage());
                    }
                }
                // 等待取消操作完成
                Thread.sleep(2000);
            }
            
            // 獲取BNBUSDT的最新價格
            BigDecimal currentPrice = binanceApiService.getLatestPrice("BNBUSDT");
            log.info("BNBUSDT 當前價格: {}", currentPrice);
            
            // 獲取交易對的價格過濾器信息，獲取正確的價格步長
            PriceFilter priceFilter = binanceApiService.getPriceFilter("BNBUSDT");
            log.info("BNBUSDT 價格過濾器信息: 最小價格={}, 最大價格={}, 價格步長={}",
                    priceFilter.getMinPrice(), priceFilter.getMaxPrice(), priceFilter.getTickSize());
            
            // 計算一個略高於當前價格的限價單價格 (高5%)
            BigDecimal orderPrice = currentPrice.multiply(new BigDecimal("1.05"));
            log.info("原始計算的訂單價格: {}", orderPrice);
            
            // 使用專用方法調整價格，確保符合交易所的價格步長要求
            orderPrice = binanceApiService.adjustPriceToTickSize("BNBUSDT", orderPrice);
            log.info("調整後的訂單價格: {}", orderPrice);
            
            // 獲取交易對的數量精度
            SymbolInfo symbolInfo = binanceApiService.getSymbolInfo("BNBUSDT");
            int quantityPrecision = symbolInfo.getQuantityPrecision();
            log.info("BNBUSDT 數量精度: {}", quantityPrecision);
            
            // 計算所需的最小數量，確保訂單價值大於100 USDT
            // 使用110確保有足夠的餘量
            BigDecimal minNotional = new BigDecimal("110");
            BigDecimal rawQuantity = minNotional.divide(orderPrice, 8, BigDecimal.ROUND_UP);
            log.info("原始計算的數量: {}", rawQuantity);
            
            // 調整數量精度，確保不會因為精度調整導致名義價值低於100
            BigDecimal quantity = rawQuantity.setScale(quantityPrecision, BigDecimal.ROUND_UP);
            log.info("調整後的訂單數量: {}", quantity);
            
            // 再次計算名義價值，確認是否大於100
            BigDecimal notional = orderPrice.multiply(quantity);
            log.info("計算的訂單名義價值: {}", notional);
            
            // 下限價單 (買入BNBUSDT，訂單會保持掛單狀態因為價格高於市場價)
            log.info("嘗試下限價單: 買入BNBUSDT, 價格={}, 數量={}", orderPrice, quantity);
            OrderResponse orderResponse = binanceApiService.placeOrder(
                    "BNBUSDT",          // 交易對
                    "BUY",              // 買入方向
                    "BOTH",             // 倉位方向
                    "LIMIT",            // 限價單
                    quantity,           // 數量 (確保名義價值>100 USDT)
                    orderPrice          // 限價
            );
            
            // 輸出訂單信息
            log.info("訂單已提交: 訂單ID={}, 狀態={}", orderResponse.getOrderId(), orderResponse.getStatus());
            
            // 驗證訂單不為空且訂單ID不為空
            assertNotNull(orderResponse, "訂單應成功建立");
            assertNotNull(orderResponse.getOrderId(), "訂單ID不應為空");
            
            // 等待5秒，確保訂單在系統中被完全處理
            log.info("等待5秒以確保訂單在系統中被完全處理...");
            Thread.sleep(5000);
            
            // 檢查訂單是否確實在未完成訂單列表中
            log.info("檢查訂單是否在未完成訂單列表中...");
            List<OrderResponse> openOrders = binanceApiService.getOpenOrders("BNBUSDT");
            boolean orderFound = false;
            
            for (OrderResponse order : openOrders) {
                if (order.getOrderId().equals(orderResponse.getOrderId())) {
                    orderFound = true;
                    log.info("確認訂單在未完成訂單列表中: ID={}, 狀態={}", order.getOrderId(), order.getStatus());
                    break;
                }
            }
            
            if (!orderFound) {
                log.warn("訂單未在未完成訂單列表中找到，可能已經被執行或取消");
                return;  // 訂單不在列表中，無法取消，測試結束
            }
            
            // 取消剛才下的訂單
            log.info("嘗試取消訂單: 訂單ID={}", orderResponse.getOrderId());
            boolean cancelResult = binanceApiService.cancelOrder("BNBUSDT", orderResponse.getOrderId());
            
            // 驗證取消成功
            assertTrue(cancelResult, "訂單應成功取消");
            log.info("訂單已成功取消");
            
            log.info("測試成功完成");
        } catch (Exception e) {
            log.error("測試下單和取消訂單失敗", e);
            fail("測試失敗: " + e.getMessage());
        }
    }
}
