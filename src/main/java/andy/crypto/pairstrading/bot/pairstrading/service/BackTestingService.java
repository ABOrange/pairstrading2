package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.pairstrading.model.PairsTradingResult;
import java.util.List;
import java.util.Map;

/**
 * 回測服務，負責回測功能
 */
public interface BackTestingService {
    
    /**
     * 回測單個交易對組合
     */
    PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays);
    
    /**
     * 回測單個交易對組合（帶有時間間隔參數）
     */
    PairsTradingResult backTestPairCombination(String symbol1, String symbol2, Integer backTestDays, String interval);
    
    /**
     * 批量回測多個交易對組合（帶有時間間隔參數）
     */
    Map<String, PairsTradingResult> batchBackTestPairCombinations(List<String> pairCombinations, Integer backTestDays, String interval);
    
    /**
     * 批量回測所有已儲存的交易對組合（帶有時間間隔參數）
     */
    Map<String, PairsTradingResult> backTestAllSavedPairCombinations(Integer backTestDays, String interval);
    
    /**
     * 獲取交易對回測分析結果的Z分數圖表
     */
    String getBackTestZScoreChart(String symbol1, String symbol2);
    
    /**
     * 獲取交易對回測分析結果的價差圖表
     */
    String getBackTestSpreadChart(String symbol1, String symbol2);
    
    /**
     * 計算回測時間長度
     */
    int calculateBackTestTimeLength(Integer backTestDays);
    
    /**
     * 計算回測歷史數據中的成功套利次數
     */
    int countSuccessfulTradesInBackTest(List<Double> zScoreHistory, double entryThreshold, double exitThreshold);
}
