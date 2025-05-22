package andy.crypto.pairstrading.bot.pairstrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 訂單響應模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private String symbol; // 交易對符號
    private Long orderId; // 訂單ID
    private String clientOrderId; // 客戶端訂單ID
    private BigDecimal price; // 價格
    private BigDecimal origQty; // 原始數量
    private BigDecimal executedQty; // 已執行數量
    private String status; // 訂單狀態
    private String type; // 訂單類型
    private String side; // 訂單方向 (BUY, SELL)
    private BigDecimal avgPrice; // 平均成交價格
    private String positionSide; // 倉位方向 (LONG, SHORT, BOTH)
    private Long updateTime; // 更新時間
    
    @Override
    public String toString() {
        return "OrderResponse{" +
                "symbol='" + symbol + '\'' +
                ", orderId=" + orderId +
                ", clientOrderId='" + clientOrderId + '\'' +
                ", price=" + price +
                ", origQty=" + origQty +
                ", executedQty=" + executedQty +
                ", status='" + status + '\'' +
                ", type='" + type + '\'' +
                ", side='" + side + '\'' +
                ", avgPrice=" + avgPrice +
                ", positionSide='" + positionSide + '\'' +
                '}';
    }
}
