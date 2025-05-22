package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import andy.crypto.pairstrading.bot.pairstrading.model.PairsTradingResult;
import java.util.List;
import java.util.Map;

/**
 * 配對交易服務介面 - 門面模式
 */
public interface PairsTradingService {
    
    /**
     * 獲取市場數據
     */
    PairsTradingServiceValueBean fetchMarketData();

    /**
     * 獲取市場數據
     */
    PairsTradingServiceValueBean fetchMarketData(String asset1 , String asset2);
    
    /**
     * 分析配對資產相關性
     */
    void analyzeCorrelation(PairsTradingServiceValueBean pairsTradingServiceValueBean);
    
    /**
     * 計算交易信號
     * @return 是否有交易信號
     */
    boolean calculateSignal(PairsTradingServiceValueBean pairsTradingServiceValueBean);
    
    /**
     * 執行交易
     * @param isBuy 是買入還是賣出信號
     */
    void executeTrade(boolean isBuy);
    
    /**
     * 獲取Z分數圖表的ASCII表示
     * @return Z分數圖表字符串
     */
    String getZScoreChart(String asset1, String asset2);

    
    /**
     * 獲取交易信號報告
     * @return 信號報告字符串
     */
    String getSignalReport();

    /**
     * 設置窗口大小
     * @param newWindowSize 新的窗口大小
     * @return 設置結果
     */
    boolean setWindowSize(int newWindowSize);

    
    /**
     * 獲取Z分數歷史數據
     * @return Z分數歷史數據列表
     */
    List<Double> getZScoreHistory(String asset1 , String asset2);
    
    /**
     * 獲取價差歷史數據
     * @return 價差歷史數據列表
     */
    List<Double> getSpreadHistory(String asset1, String asset2);
    
    /**
     * 獲取時間歷史數據
     * @return 時間戳歷史數據列表
     */
    List<Long> getTimeHistory(String asset1, String asset2);
    
    /**
     * 獲取所有可用交易對列表
     * @return 交易對列表
     */
    List<String> getAvailableTradingPairs();
    
    /**
     * 獲取使用者已保存的交易對組合
     * @return 使用者儲存的交易對組合列表
     */
    List<String> getSavedTradingPairCombinations();
    
    /**
     * 保存交易對組合到資料庫
     * @param pairCombination 交易對組合字符串，格式如: "BTCUSDT,ETHUSDT"
     * @return 保存結果
     */
    boolean saveTradingPairCombination(String pairCombination);
    
    /**
     * 刪除已保存的交易對組合
     * @param pairCombination 要刪除的交易對組合
     * @return 刪除結果
     */
    boolean deleteTradingPairCombination(String pairCombination);
    
    /**
     * 回測單個交易對組合
     * 根據目前的系統配置，對指定的交易對組合進行回測分析
     * 
     * @param symbol1 第一個交易對符號
     * @param symbol2 第二個交易對符號
     * @param backTestDays 回測天數，如果傳入null或0，則使用系統窗口大小計算
     * @return 回測結果
     */
    PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays);
    
    /**
     * 回測單個交易對組合（帶有時間間隔參數）
     * 根據目前的系統配置，對指定的交易對組合進行回測分析
     * 
     * @param symbol1 第一個交易對符號
     * @param symbol2 第二個交易對符號
     * @param backTestDays 回測天數，如果傳入null或0，則使用系統窗口大小計算
     * @param interval 時間間隔，如 "1m", "5m", "15m", "30m", "1h", "4h", "1d"
     * @return 回測結果
     */
    PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays, String interval);
    

    /**
     * 批量回測多個交易對組合（帶有時間間隔參數）
     * 
     * @param pairCombinations 交易對組合列表，每個元素為兩個交易對的組合，格式如: ["BTCUSDT,ETHUSDT", "BNBUSDT,SOLUSDT"]
     * @param backTestDays 回測天數，如果傳入null或0，則使用系統窗口大小計算
     * @param interval 時間間隔，如 "1m", "5m", "15m", "30m", "1h", "4h", "1d"
     * @return 回測結果映射，鍵為交易對組合，值為回測結果
     */
    Map<String, PairsTradingResult> batchBackTestPairCombinations(List<String> pairCombinations, Integer backTestDays, String interval);
    

    /**
     * 批量回測所有已儲存的交易對組合（帶有時間間隔參數）
     * 
     * @param backTestDays 回測天數，如果傳入null或0，則使用系統窗口大小計算
     * @param interval 時間間隔，如 "1m", "5m", "15m", "30m", "1h", "4h", "1d"
     * @return 回測結果映射，鍵為交易對組合，值為回測結果
     */
    Map<String, PairsTradingResult> backTestAllSavedPairCombinations(Integer backTestDays, String interval);
    
    /**
     * 獲取交易對回測分析結果的Z分數圖表
     * 
     * @param symbol1 第一個交易對符號
     * @param symbol2 第二個交易對符號
     * @return Z分數圖表字符串
     */
    String getBackTestZScoreChart(String symbol1, String symbol2);
    
    /**
     * 獲取交易對回測分析結果的價差圖表
     * 
     * @param symbol1 第一個交易對符號
     * @param symbol2 第二個交易對符號
     * @return 價差圖表字符串
     */
    String getBackTestSpreadChart(String symbol1, String symbol2);

    int getWindowSize();
}
