package andy.crypto.pairstrading.bot.pairstrading.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "position_history")
public class PositionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "symbol")
    private String symbol;
    
    @Column(name = "position_side")
    private String positionSide; // LONG 或 SHORT
    
    @Column(name = "position_amt", precision = 20, scale = 8)
    private BigDecimal positionAmt;
    
    @Column(name = "entry_price", precision = 20, scale = 8)
    private BigDecimal entryPrice;
    
    @Column(name = "exit_price", precision = 20, scale = 8)
    private BigDecimal exitPrice;
    
    @Column(name = "realized_profit", precision = 20, scale = 8)
    private BigDecimal realizedProfit;
    
    @Column(name = "action")
    private String action; // OPEN 或 CLOSE
    
    @Column(name = "timestamp")
    private Long timestamp;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "z_score")
    private Double zScore;
    
    /**
     * 獲取格式化的時間
     * 
     * @return 格式化的時間字符串
     */
    public String getFormattedTime() {
        if (timestamp == null) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * 計算並設置平倉損益
     * 
     * @param exitPrice 平倉價格
     */
    public void calculateAndSetProfit(BigDecimal exitPrice) {
        this.exitPrice = exitPrice;
        
        if (positionAmt != null && entryPrice != null && exitPrice != null) {
            // 多頭: (退出價格 - 入場價格) * 倉位數量
            // 空頭: (入場價格 - 退出價格) * 倉位數量的絕對值
            if (positionAmt.compareTo(BigDecimal.ZERO) > 0) {
                // 多頭倉位
                this.realizedProfit = exitPrice.subtract(entryPrice).multiply(positionAmt);
            } else if (positionAmt.compareTo(BigDecimal.ZERO) < 0) {
                // 空頭倉位
                this.realizedProfit = entryPrice.subtract(exitPrice).multiply(positionAmt.abs());
            } else {
                this.realizedProfit = BigDecimal.ZERO;
            }
        } else {
            this.realizedProfit = BigDecimal.ZERO;
        }
    }
    
    /**
     * 在持久化前確保損益已計算
     * 如果是平倉操作但尚未設置損益，則根據現有數據計算損益
     */
    @PrePersist
    public void ensureProfitCalculated() {
        if ("CLOSE".equals(action) && realizedProfit == null && exitPrice != null && entryPrice != null) {
            calculateAndSetProfit(exitPrice);
        }
    }
}
