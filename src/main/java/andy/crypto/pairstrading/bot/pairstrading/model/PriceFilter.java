package andy.crypto.pairstrading.bot.pairstrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 交易對價格過濾器
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceFilter {
    
    private BigDecimal minPrice; // 最小價格
    private BigDecimal maxPrice; // 最大價格
    private BigDecimal tickSize; // 價格步長
    
    @Override
    public String toString() {
        return "PriceFilter{" +
                "minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", tickSize=" + tickSize +
                '}';
    }
}
