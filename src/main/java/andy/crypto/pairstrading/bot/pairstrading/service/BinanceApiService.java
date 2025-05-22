package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;
import andy.crypto.pairstrading.bot.pairstrading.model.OrderResponse;
import andy.crypto.pairstrading.bot.pairstrading.model.PositionInfo;
import andy.crypto.pairstrading.bot.pairstrading.model.PriceFilter;
import andy.crypto.pairstrading.bot.pairstrading.model.SymbolInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 幣安永續合約API服務介面
 */
public interface BinanceApiService {
    
    /**
     * 獲取帳戶餘額
     * @return 可用餘額，key為幣種，value為餘額金額
     */
    Map<String, BigDecimal> getAccountBalance();
    
    /**
     * 獲取指定交易對的最新價格
     * @param symbol 交易對符號，例如 "BNBUSDT"
     * @return 最新價格
     */
    BigDecimal getLatestPrice(String symbol);
    
    /**
     * 獲取K線數據
     * @param symbol 交易對符號
     * @param interval K線間隔，例如 "1m", "5m", "1h"
     * @param limit 返回的記錄數量
     * @return K線數據列表
     */
    List<CandlestickData> getCandlestickData(String symbol, String interval, Integer limit);
    
    /**
     * 獲取交易對基本資訊
     * @param symbol 交易對符號
     * @return 交易對資訊
     */
    SymbolInfo getSymbolInfo(String symbol);
    
    /**
     * 獲取持倉信息
     * @param symbol 可選，交易對符號。如果為null，則返回所有持倉
     * @return 持倉信息列表
     */
    List<PositionInfo> getPositionInfo(String symbol);
    
    /**
     * 下單
     * @param symbol 交易對符號
     * @param side 交易方向，"BUY" 或 "SELL"
     * @param positionSide 倉位方向，"LONG" 或 "SHORT"
     * @param type 訂單類型，例如 "MARKET", "LIMIT"
     * @param quantity 數量
     * @param price 價格，對於市價單可以為null
     * @return 訂單回應
     */
    OrderResponse placeOrder(String symbol, String side, String positionSide, String type, BigDecimal quantity, BigDecimal price);
    
    /**
     * 取消訂單
     * @param symbol 交易對符號
     * @param orderId 訂單ID
     * @return 是否成功取消
     */
    boolean cancelOrder(String symbol, Long orderId);
    
    /**
     * 獲取未完成訂單
     * @param symbol 交易對符號
     * @return 訂單列表
     */
    List<OrderResponse> getOpenOrders(String symbol);
    
    /**
     * 關閉指定交易對的所有倉位
     * @param symbol 交易對符號
     * @return 是否成功關閉
     */
    boolean closeAllPositions(String symbol);
    
    /**
     * 獲取交易對的價格過濾器信息
     * @param symbol 交易對符號
     * @return 價格過濾器信息，包含價格步長(tickSize)
     */
    PriceFilter getPriceFilter(String symbol);
    
    /**
     * 設定合約槓桿倍率
     * @param symbol 交易對符號
     * @param leverage 槓桿倍率
     * @return 是否設定成功
     */
    boolean setLeverage(String symbol, int leverage);
    
    /**
     * 將價格調整為符合交易對價格步長的有效價格
     * @param symbol 交易對符號
     * @param price 原始價格
     * @return 調整後的價格
     */
    BigDecimal adjustPriceToTickSize(String symbol, BigDecimal price);
    
    /**
     * 將數量調整為符合交易對數量精度的有效數量
     * @param symbol 交易對符號
     * @param quantity 原始數量
     * @return 調整後的數量
     */
    BigDecimal adjustQuantityToPrecision(String symbol, BigDecimal quantity);
    
    /**
     * 獲取所有可用的永續合約交易對
     * @return 交易對符號列表
     */
    List<String> getAvailableFuturesPairs();
}
