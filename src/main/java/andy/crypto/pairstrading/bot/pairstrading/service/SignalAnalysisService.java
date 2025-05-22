package andy.crypto.pairstrading.bot.pairstrading.service;

import andy.crypto.pairstrading.bot.bean.PairsTradingServiceValueBean;

/**
 * 信號分析服務，負責分析市場數據，生成交易信號
 */
public interface SignalAnalysisService {
    
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
     * 獲取窗口大小
     */
    int getWindowSize();
}
