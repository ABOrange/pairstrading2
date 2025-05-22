package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;
import java.math.BigDecimal;

/**
 * 交易執行服務，負責執行交易操作
 */
public interface TradingExecutionService {
    
    /**
     * 執行交易
     * @param isBuy 是買入還是賣出信號
     */
    void executeTrade(boolean isBuy);
    
    /**
     * 執行配對交易倉位
     * @param asset1 第一個資產
     * @param asset2 第二個資產
     * @param isLongAsset1 是否做多資產1
     * @param isLongAsset2 是否做多資產2
     * @param reason 交易原因描述
     */
    void executePairedPositions(String asset1, String asset2, boolean isLongAsset1, boolean isLongAsset2, String reason);
    
    /**
     * 計算交易數量
     * @param symbol 交易對符號
     * @return 調整後的交易數量
     */
    BigDecimal calculateQuantity(String symbol, PairsTradingServiceValueBean pairsTradingServiceValueBean);
    
    /**
     * 為交易對設定槓桿倍率
     */
    void setLeverageForSymbols();
}
