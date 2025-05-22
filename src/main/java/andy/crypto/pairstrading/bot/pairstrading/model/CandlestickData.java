package andy.crypto.pairstrading.bot.pairstrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * K線數據模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandlestickData {
    
    private Long openTime; // 開盤時間
    private BigDecimal open; // 開盤價
    private BigDecimal high; // 最高價
    private BigDecimal low; // 最低價
    private BigDecimal close; // 收盤價
    private BigDecimal volume; // 成交量
    private Long closeTime; // 收盤時間
    private BigDecimal quoteAssetVolume; // 報價資產成交量
    private Integer numberOfTrades; // 成交筆數
    private BigDecimal takerBuyBaseAssetVolume; // Taker買入基礎資產數量
    private BigDecimal takerBuyQuoteAssetVolume; // Taker買入報價資產數量
    
    @Override
    public String toString() {
        return "CandlestickData{" +
                "openTime=" + openTime +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", closeTime=" + closeTime +
                '}';
    }
}
