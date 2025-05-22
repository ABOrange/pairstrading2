package andy.crypto.pairstrading.bot.pairstrading.util;

import java.util.List;

/**
 * 控制台圖表工具類
 */
public class ConsoleChartUtil {
    
    private static final int CHART_WIDTH = 60;
    private static final int CHART_HEIGHT = 15;
    private static final char CHART_CHAR = '█';
    private static final char THRESHOLD_CHAR = '·';
    private static final char MEAN_CHAR = '-';
    
    /**
     * 繪製Z分數圖表
     * 
     * @param data Z分數數據
     * @param entryThreshold 入場閾值
     * @param exitThreshold 出場閾值
     * @return 圖表字符串
     */
    public static String drawZScoreChart(List<Double> data, double entryThreshold, double exitThreshold) {
        if (data == null || data.isEmpty()) {
            return "無數據可繪製";
        }
        
        int dataSize = data.size();
        int displayPoints = Math.min(dataSize, CHART_WIDTH);
        
        // 採樣數據以適應顯示寬度
        double[] sampledData = sampleData(data, displayPoints);
        
        // 計算最大值和最小值
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        
        for (double value : sampledData) {
            if (value > max) max = value;
            if (value < min) min = value;
        }
        
        // 確保閾值在範圍內
        max = Math.max(max, Math.max(entryThreshold, -exitThreshold));
        min = Math.min(min, Math.min(-entryThreshold, exitThreshold));
        
        // 增加一點緩衝區
        double buffer = Math.max((max - min) * 0.1, 0.5);
        max += buffer;
        min -= buffer;
        
        StringBuilder chart = new StringBuilder();
        
        // 繪製圖表標題
        chart.append(String.format("Z分數圖表 (最新值: %.2f, 閾值: 入場 ±%.1f, 出場 ±%.1f)\n", 
                data.get(data.size() - 1), entryThreshold, exitThreshold));
        
        // 繪製Y軸刻度
        chart.append(String.format("%5.1f┐\n", max));
        
        // 繪製主體圖表
        for (int y = CHART_HEIGHT - 1; y >= 0; y--) {
            double yValue = min + (max - min) * y / (CHART_HEIGHT - 1);
            
            // 繪製Y軸刻度
            if (y == CHART_HEIGHT / 2) {
                chart.append(String.format("%5.1f┤", yValue));
            } else {
                chart.append("     │");
            }
            
            // 繪製圖表內容
            for (int x = 0; x < displayPoints; x++) {
                double value = sampledData[x];
                double nextYValue = min + (max - min) * (y - 1) / (CHART_HEIGHT - 1);
                
                // 繪製入場閾值線
                if ((Math.abs(yValue - entryThreshold) < Math.abs(yValue - nextYValue) / 2) || 
                    (Math.abs(yValue + entryThreshold) < Math.abs(yValue - nextYValue) / 2)) {
                    chart.append(THRESHOLD_CHAR);
                }
                // 繪製出場閾值線
                else if ((Math.abs(yValue - exitThreshold) < Math.abs(yValue - nextYValue) / 2) || 
                         (Math.abs(yValue + exitThreshold) < Math.abs(yValue - nextYValue) / 2)) {
                    chart.append(THRESHOLD_CHAR);
                }
                // 繪製零線
                else if (Math.abs(yValue) < Math.abs(yValue - nextYValue) / 2) {
                    chart.append(MEAN_CHAR);
                }
                // 繪製數據點
                else if (value >= yValue && value < yValue + (max - min) / (CHART_HEIGHT - 1)) {
                    chart.append(CHART_CHAR);
                } 
                else {
                    chart.append(' ');
                }
            }
            
            chart.append('\n');
        }
        
        // 繪製X軸
        chart.append(String.format("%5.1f┴", min));
        chart.append("─".repeat(displayPoints)).append('\n');
        
        // 繪製X軸標籤（顯示最早和最新時間點）
        chart.append("     過去").append(" ".repeat(displayPoints - 10)).append("最新\n");
        
        return chart.toString();
    }
    
    /**
     * 繪製價差圖表
     * 
     * @param data 價差數據
     * @param mean 均值
     * @param std 標準差
     * @return 圖表字符串
     */
    public static String drawSpreadChart(List<Double> data, double mean, double std) {
        if (data == null || data.isEmpty()) {
            return "無數據可繪製";
        }
        
        int dataSize = data.size();
        int displayPoints = Math.min(dataSize, CHART_WIDTH);
        
        // 採樣數據以適應顯示寬度
        double[] sampledData = sampleData(data, displayPoints);
        
        // 計算最大值和最小值
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        
        for (double value : sampledData) {
            if (value > max) max = value;
            if (value < min) min = value;
        }
        
        // 確保均值和標準差範圍在圖表範圍內
        max = Math.max(max, mean + 2 * std);
        min = Math.min(min, mean - 2 * std);
        
        // 增加一點緩衝區
        double buffer = (max - min) * 0.1;
        max += buffer;
        min -= buffer;
        
        StringBuilder chart = new StringBuilder();
        
        // 繪製圖表標題
        chart.append(String.format("價差圖表 (最新值: %.2f, 均值: %.2f, 標準差: %.2f)\n", 
                data.get(data.size() - 1), mean, std));
        
        // 繪製Y軸刻度
        chart.append(String.format("%6.1f┐\n", max));
        
        // 繪製主體圖表
        for (int y = CHART_HEIGHT - 1; y >= 0; y--) {
            double yValue = min + (max - min) * y / (CHART_HEIGHT - 1);
            
            // 繪製Y軸刻度
            if (y == CHART_HEIGHT / 2) {
                chart.append(String.format("%6.1f┤", yValue));
            } else {
                chart.append("      │");
            }
            
            // 繪製圖表內容
            for (int x = 0; x < displayPoints; x++) {
                double value = sampledData[x];
                double nextYValue = min + (max - min) * (y - 1) / (CHART_HEIGHT - 1);
                
                // 繪製均值線
                if (Math.abs(yValue - mean) < Math.abs(yValue - nextYValue) / 2) {
                    chart.append(MEAN_CHAR);
                }
                // 繪製均值+標準差線
                else if (Math.abs(yValue - (mean + std)) < Math.abs(yValue - nextYValue) / 2 || 
                         Math.abs(yValue - (mean - std)) < Math.abs(yValue - nextYValue) / 2) {
                    chart.append(THRESHOLD_CHAR);
                }
                // 繪製均值+2倍標準差線
                else if (Math.abs(yValue - (mean + 2 * std)) < Math.abs(yValue - nextYValue) / 2 || 
                         Math.abs(yValue - (mean - 2 * std)) < Math.abs(yValue - nextYValue) / 2) {
                    chart.append(THRESHOLD_CHAR);
                }
                // 繪製數據點
                else if (value >= yValue && value < yValue + (max - min) / (CHART_HEIGHT - 1)) {
                    chart.append(CHART_CHAR);
                } 
                else {
                    chart.append(' ');
                }
            }
            
            chart.append('\n');
        }
        
        // 繪製X軸
        chart.append(String.format("%6.1f┴", min));
        chart.append("─".repeat(displayPoints)).append('\n');
        
        // 繪製X軸標籤（顯示最早和最新時間點）
        chart.append("      過去").append(" ".repeat(displayPoints - 10)).append("最新\n");
        
        return chart.toString();
    }
    
    /**
     * 生成交易信號報告
     * 
     * @param zScore 當前Z分數
     * @param entryThreshold 入場閾值
     * @param exitThreshold 出場閾值
     * @param asset1 資產1
     * @param asset2 資產2
     * @param correlation 相關性
     * @param hasPositions 是否有倉位
     * @param alpha 回歸截距
     * @param beta 回歸斜率
     * @return 報告字符串
     */
    public static String generateSignalReport(double zScore, double entryThreshold, double exitThreshold, 
                                             String asset1, String asset2, double correlation, boolean hasPositions,
                                             double alpha, double beta) {
        StringBuilder report = new StringBuilder();
        
        report.append("══════════════ 交易信號報告 ══════════════\n");
        report.append(String.format("Z分數: %.2f\n", zScore));
        report.append(String.format("相關性: %.2f\n", correlation));
        report.append(String.format("入場閾值: ±%.1f\n", entryThreshold));
        report.append(String.format("出場閾值: ±%.1f\n", exitThreshold));
        report.append(String.format("當前持倉: %s\n", hasPositions ? "有" : "無"));
        report.append(String.format("回歸模型: %s = %.4f + %.4f * %s\n", asset1, alpha, beta, asset2));
        report.append("──────────────────────────────────────\n");
        
        // 判斷交易信號
        if (zScore > entryThreshold) {
            report.append(String.format("入場信號: 做空 %s, 做多 %s\n", asset1, asset2));
        } else if (zScore < -entryThreshold) {
            report.append(String.format("入場信號: 做多 %s, 做空 %s\n", asset1, asset2));
        } else if (Math.abs(zScore) < exitThreshold && hasPositions) {
            report.append("出場信號: 平倉所有倉位\n");
        } else {
            report.append("當前無交易信號\n");
        }
        
        report.append("──────────────────────────────────────\n");
        
        // 信號強度判斷
        String strength;
        if (Math.abs(zScore) > entryThreshold * 1.5) {
            strength = "強";
        } else if (Math.abs(zScore) > entryThreshold) {
            strength = "中";
        } else if (Math.abs(zScore) > exitThreshold) {
            strength = "弱";
        } else {
            strength = "無";
        }
        
        report.append(String.format("信號強度: %s\n", strength));
        
        // 相關性判斷
        String correlationStrength;
        if (Math.abs(correlation) > 0.9) {
            correlationStrength = "非常強";
        } else if (Math.abs(correlation) > 0.7) {
            correlationStrength = "強";
        } else if (Math.abs(correlation) > 0.5) {
            correlationStrength = "中";
        } else if (Math.abs(correlation) > 0.3) {
            correlationStrength = "弱";
        } else {
            correlationStrength = "很弱";
        }
        
        report.append(String.format("相關性強度: %s\n", correlationStrength));
        report.append("══════════════════════════════════════\n");
        
        return report.toString();
    }
    
    /**
     * 生成交易信號報告（舊版本，保持向後兼容）
     * 
     * @param zScore 當前Z分數
     * @param entryThreshold 入場閾值
     * @param exitThreshold 出場閾值
     * @param asset1 資產1
     * @param asset2 資產2
     * @param correlation 相關性
     * @param hasPositions 是否有倉位
     * @return 報告字符串
     */
    public static String generateSignalReport(double zScore, double entryThreshold, double exitThreshold, 
                                             String asset1, String asset2, double correlation, boolean hasPositions) {
        // 使用預設的alpha和beta (舊版本沒有這些參數)
        return generateSignalReport(zScore, entryThreshold, exitThreshold, asset1, asset2, correlation, hasPositions, 0.0, 1.0);
    }
    
    /**
     * 對數據進行採樣以適應顯示寬度
     * 
     * @param data 原始數據
     * @param sampleSize 採樣大小
     * @return 採樣後的數據
     */
    private static double[] sampleData(List<Double> data, int sampleSize) {
        int dataSize = data.size();
        double[] sampledData = new double[sampleSize];
        
        if (dataSize <= sampleSize) {
            // 數據點不足，直接複製
            for (int i = 0; i < dataSize; i++) {
                sampledData[i] = data.get(i);
            }
            
            // 空位補最後一個值
            for (int i = dataSize; i < sampleSize; i++) {
                sampledData[i] = data.get(dataSize - 1);
            }
        } else {
            // 數據點過多，需要採樣
            double step = (double) dataSize / sampleSize;
            
            for (int i = 0; i < sampleSize; i++) {
                int index = Math.min(dataSize - 1, (int) (i * step));
                sampledData[i] = data.get(index);
            }
        }
        
        return sampledData;
    }
}
