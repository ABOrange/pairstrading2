package andy.crypto.pairstrading.bot.bean;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradingConfigBean {
    private String asset1;
    private String asset2;
    private int windowSize;
    private double entryThreshold;
    private double exitThreshold;
    private BigDecimal positionSize;
    private boolean isAmountBased;
    private int leverage;
    private boolean tradingEnabled;
    private boolean consoleChartEnabled;
    private boolean consoleSignalEnabled;
}
