package andy.crypto.pairstrading.bot.pairstrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 倉位信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionInfo {

    private String symbol; // 交易對符號
    private String positionSide; // 倉位方向 (LONG, SHORT, BOTH)
    private BigDecimal positionAmt; // 倉位數量
    private BigDecimal entryPrice; // 入場價格
    private BigDecimal markPrice; // 標記價格
    private BigDecimal liquidationPrice; // 強平價格
    private BigDecimal unrealizedProfit; // 未實現盈虧
    private String unrealizedProfitPercentage;
    private BigDecimal leverage; // 槓桿倍率
    private Long updateTime; // 更新時間
    private boolean isolated; // 是否為逐倉模式

    // 用於模擬倉位
    @Builder.Default
    private boolean isSimulated = false;

    /**
     * 判斷是否為多頭倉位
     *
     * @return 是否為多頭倉位
     */
    public boolean isLongPosition() {
        if (positionAmt == null) {
            return false;
        }
        return positionAmt.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判斷是否為空頭倉位
     *
     * @return 是否為空頭倉位
     */
    public boolean isShortPosition() {
        if (positionAmt == null) {
            return false;
        }
        return positionAmt.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 獲取倉位的絕對值大小
     *
     * @return 倉位的絕對值大小
     */
    public BigDecimal getAbsolutePositionSize() {
        if (positionAmt == null) {
            return BigDecimal.ZERO;
        }
        return positionAmt.abs();
    }

    /**
     * 創建模擬倉位
     *
     * @param symbol 交易對符號
     * @param positionSide 倉位方向
     * @param positionAmt 倉位數量
     * @param entryPrice 入場價格
     * @return 模擬倉位
     */
    public static PositionInfo createSimulated(String symbol, String positionSide, BigDecimal positionAmt, BigDecimal entryPrice) {
        return PositionInfo.builder()
                .symbol(symbol)
                .positionSide(positionSide)
                .positionAmt(positionAmt)
                .entryPrice(entryPrice)
                .isSimulated(true)
                .updateTime(System.currentTimeMillis())
                .build();
    }
}
