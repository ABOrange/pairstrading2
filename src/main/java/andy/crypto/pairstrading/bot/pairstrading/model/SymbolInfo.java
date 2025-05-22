package andy.crypto.pairstrading.bot.pairstrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 交易對基本資訊
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolInfo {
    
    private String symbol; // 交易對符號
    private String pair; // 交易對
    private String contractType; // 合約類型
    private Long deliveryDate; // 交割日期
    private Long onboardDate; // 上線日期
    private String status; // 交易對狀態
    private String baseAsset; // 基礎資產
    private String quoteAsset; // 報價資產
    private String marginAsset; // 保證金資產
    private Integer pricePrecision; // 價格精度
    private Integer quantityPrecision; // 數量精度
    private BigDecimal baseAssetPrecision; // 基礎資產精度
    private BigDecimal quotePrecision; // 報價資產精度
    private BigDecimal maintMarginPercent; // 維持保證金比例
    private BigDecimal requiredMarginPercent; // 所需保證金比例
    private PriceFilter priceFilter; // 價格過濾器
    private BigDecimal minNotional; // 最小名義價值
    
    @Override
    public String toString() {
        return "SymbolInfo{" +
                "symbol='" + symbol + '\'' +
                ", pair='" + pair + '\'' +
                ", contractType='" + contractType + '\'' +
                ", status='" + status + '\'' +
                ", baseAsset='" + baseAsset + '\'' +
                ", quoteAsset='" + quoteAsset + '\'' +
                ", pricePrecision=" + pricePrecision +
                ", quantityPrecision=" + quantityPrecision +
                ", priceFilter=" + priceFilter +
                ", minNotional=" + minNotional +
                '}';
    }
}
