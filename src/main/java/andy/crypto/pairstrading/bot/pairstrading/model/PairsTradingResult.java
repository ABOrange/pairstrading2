package andy.crypto.pairstrading.bot.pairstrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配對交易結果模型
 * 用於儲存配對交易的結果數據
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairsTradingResult {

    /**
     * 結果ID
     */
    private String id;
    
    /**
     * 結果產生時間
     */
    private LocalDateTime timestamp;
    
    /**
     * 第一個資產代碼
     */
    private String asset1;
    
    /**
     * 第二個資產代碼
     */
    private String asset2;
    
    /**
     * 兩資產相關性係數
     */
    private double correlation;
    
    /**
     * 回歸係數 (Beta)
     */
    private double beta;
    
    /**
     * 價差
     */
    private double spread;
    
    /**
     * 價差平均值
     */
    private double spreadMean;
    
    /**
     * 價差標準差
     */
    private double spreadStd;
    
    /**
     * Z分數
     */
    private double zScore;
    
    /**
     * 資產1的最新價格
     */
    private BigDecimal asset1Price;
    
    /**
     * 資產2的最新價格
     */
    private BigDecimal asset2Price;
    
    /**
     * 交易信號類型
     * LONG_ASSET1_SHORT_ASSET2: 做多資產1，做空資產2
     * SHORT_ASSET1_LONG_ASSET2: 做空資產1，做多資產2
     * CLOSE_POSITIONS: 平倉所有倉位
     * NO_SIGNAL: 沒有交易信號
     */
    private SignalType signalType;
    
    /**
     * 信號強度評級
     */
    private String signalRating;
    
    /**
     * Z分數圖表ASCII文本
     */
    private String zScoreChart;
    
    /**
     * 價差圖表ASCII文本
     */
    private String spreadChart;
    
    /**
     * 交易信號報告
     */
    private String signalReport;
    
    /**
     * 回測使用的時間間隔
     * 例如: "1m", "5m", "15m", "30m", "1h", "4h", "1d"
     */
    private String interval;
    
    /**
     * 回測期間成功套利次數
     */
    private int arbitrageCount;
    
    /**
     * 回測期間爆倉次數
     */
    private int liquidationCount;
    
    /**
     * ADF平穩性檢定結果
     * true: 通過檢定
     * false: 未通過檢定
     */
    private boolean stationaryTest;
    
    /**
     * 信號類型枚舉
     */
    public enum SignalType {
        LONG_ASSET1_SHORT_ASSET2("做多%s，做空%s"),
        SHORT_ASSET1_LONG_ASSET2("做空%s，做多%s"),
        CLOSE_POSITIONS("平倉所有倉位"),
        NO_SIGNAL("沒有交易信號");
        
        private String description;
        
        SignalType(String description) {
            this.description = description;
        }
        
        public String getFormatted(String asset1, String asset2) {
            return String.format(description, asset1, asset2);
        }
    }
}
