package andy.crypto.pairstrading.bot.pairstrading.service;

import java.util.List;

/**
 * 交易對管理服務，負責交易對組合的管理
 */
public interface TradingPairManagementService {
    
    /**
     * 獲取使用者已保存的交易對組合
     */
    List<String> getSavedTradingPairCombinations();
    
    /**
     * 保存交易對組合到資料庫
     */
    boolean saveTradingPairCombination(String pairCombination);
    
    /**
     * 刪除已保存的交易對組合
     */
    boolean deleteTradingPairCombination(String pairCombination);
    
    /**
     * 驗證交易對組合格式和可用性
     */
    boolean validateTradingPairCombination(String pairCombination);
}
