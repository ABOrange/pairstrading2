package andy.crypto.pairstrading.bot.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PairsTradingServiceValueBean {
    private double lastZScore = 0.0;
    private double alpha = 0.0; // 回歸模型截距項
    private double normalizedCorrelation = 0.0;
    private double beta = 0.0;  // 回歸模型斜率
    private double spread = 0.0;
    private double spreadMean = 0.0;
    private double spreadStd = 0.0;
    private double correlation = 0.0;
    private boolean stationaryTest = false; // ADF 平穩性檢定結果

    protected List<Double> spreadHistory = new ArrayList<>();
    protected List<Double> zScoreHistory = new ArrayList<>();
    protected List<Long> timeHistory = new ArrayList<>();

    // 暫存最後生成的圖表和報告
    private String lastZScoreChart = "";
    private String lastSpreadChart = "";
    private String lastSignalReport = "";
}
