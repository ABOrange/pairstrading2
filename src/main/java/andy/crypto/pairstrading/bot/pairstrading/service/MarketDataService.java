package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.model.CandlestickData;

import java.util.List;

/**
 * 市場數據服務，負責獲取和處理市場數據
 */
public interface MarketDataService {
    
    /**
     * 獲取市場數據
     */
    PairsTradingServiceValueBean fetchMarketData();

    /**
     * 獲取市場數據（指定資產對）
     */
    PairsTradingServiceValueBean fetchMarketData(String asset1, String asset2);
    
    /**
     * 更新價格數據並計算配對交易相關指標
     */
    PairsTradingServiceValueBean updatePriceData(double[] asset1Prices, double[] asset2Prices, List<CandlestickData> asset1Data);
    
    /**
     * 獲取Z分數歷史數據
     */
    List<Double> getZScoreHistory(String asset1, String asset2);
    
    /**
     * 獲取價差歷史數據
     */
    List<Double> getSpreadHistory(String asset1, String asset2);
    
    /**
     * 獲取時間歷史數據
     */
    List<Long> getTimeHistory(String asset1, String asset2);
    
    /**
     * 獲取所有可用交易對列表
     */
    List<String> getAvailableTradingPairs();
    
    /**
     * 獲取Z分數圖表的ASCII表示
     */
    String getZScoreChart(String asset1, String asset2);
}
