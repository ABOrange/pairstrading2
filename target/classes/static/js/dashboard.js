function updateZScoreChart(data) {
    if (!data || !data.zScores || !data.labels) {
        return;
    }
    
    const zScores = data.zScores;
    const labels = data.labels;
    const entryThreshold = data.entryThreshold || 2.0;
    const exitThreshold = data.exitThreshold || 0.5;
    
    // 確保zScoreChart已初始化
    if (zScoreChart) {
        // 更新數據前先備份當前配置
        const currentOptions = { ...zScoreChart.options };
        
        // 更新數據
        zScoreChart.data.labels = labels;
        zScoreChart.data.datasets[0].data = zScores;
        
        // 更新閾值線
        const entryThresholdLine = Array(labels.length).fill(entryThreshold);
        const entryThresholdLowerLine = Array(labels.length).fill(-entryThreshold);
        const exitThresholdLine = Array(labels.length).fill(exitThreshold);
        const exitThresholdLowerLine = Array(labels.length).fill(-exitThreshold);
        const zeroLine = Array(labels.length).fill(0);
        
        zScoreChart.data.datasets[1].data = entryThresholdLine;
        zScoreChart.data.datasets[2].data = entryThresholdLowerLine;
        zScoreChart.data.datasets[3].data = exitThresholdLine;
        zScoreChart.data.datasets[4].data = exitThresholdLowerLine;
        zScoreChart.data.datasets[5].data = zeroLine;
        
        // 確保縮放設置不變
        zScoreChart.options.scales.y.min = -4;
        zScoreChart.options.scales.y.max = 4;
        
        // 更新圖表，但不自動縮放
        zScoreChart.update({
            duration: 500,
            easing: 'easeOutQuad'
        });
    }
}

/**
 * 更新價差圖表
 * 
 * @param {Object} data 圖表數據
 */
function updateSpreadChart(data) {
    if (!data || !data.spreads || !data.labels) {
        return;
    }
    
    const spreads = data.spreads;
    const labels = data.labels;
    const spreadMean = data.spreadMean || 0;
    const spreadStd = data.spreadStd || 1;
    
    // 確保spreadChart已初始化
    if (spreadChart) {
        // 更新數據
        spreadChart.data.labels = labels;
        spreadChart.data.datasets[0].data = spreads;
        
        // 更新均值和標準差線
        const meanLine = Array(labels.length).fill(spreadMean);
        const upperStdLine = Array(labels.length).fill(spreadMean + spreadStd);
        const lowerStdLine = Array(labels.length).fill(spreadMean - spreadStd);
        const upper2StdLine = Array(labels.length).fill(spreadMean + 2 * spreadStd);
        const lower2StdLine = Array(labels.length).fill(spreadMean - 2 * spreadStd);
        
        spreadChart.data.datasets[1].data = meanLine;
        spreadChart.data.datasets[2].data = upperStdLine;
        spreadChart.data.datasets[3].data = lowerStdLine;
        spreadChart.data.datasets[4].data = upper2StdLine;
        spreadChart.data.datasets[5].data = lower2StdLine;
        
        // 更新圖表
        spreadChart.update();
    }
}

/**
 * 刷新系統日誌
 */
function refreshLogs() {
    // 重新啟動倒數計時
    startCountdownTimer();
    
    $.ajax({
        url: '/api/logs/system',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                const logsContainer = $('#systemLogs');
                logsContainer.empty();
                
                if (response.logs && response.logs.length > 0) {
                    response.logs.forEach(function(log) {
                        const logLine = $('<div></div>').text(log);
                        
                        // 根據日誌級別添加顏色
                        if (log.includes('[ERROR]')) {
                            logLine.addClass('text-danger');
                        } else if (log.includes('[WARN]')) {
                            logLine.addClass('text-warning');
                        } else if (log.includes('[INFO]')) {
                            logLine.addClass('text-info');
                        }
                        
                        logsContainer.append(logLine);
                    });
                } else {
                    logsContainer.text('暫無日誌');
                }
                
                // 滾動到底部
                logsContainer.scrollTop(logsContainer.prop('scrollHeight'));
            }
        }
    });
}

/**
 * 刷新數據但不重置倒數計時
 */
function refreshDataWithoutResetTimer() {
    // 獲取當前激活的標籤頁
    const activeTab = $('.nav-link.active').attr('href');
    
    // 根據當前標籤頁執行對應的刷新函數，但跳過重置計時器的部分
    // 使用內部函數來避免觸發重置倒數
    
    // 刷新總覽頁
    if (activeTab === '#overview') {
        // 獲取交易信號報告
        $.ajax({
            url: '/api/charts/signal-report',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success') {
                    $('#signalReport').text(response.report || '暫無數據');
                }
            }
        });
        
        // 獲取最新價格
        $.ajax({
            url: '/api/market/prices',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success' && response.prices) {
                    const prices = response.prices;
                    
                    // 更新頁面上的價格顯示
                    for (const [symbol, price] of Object.entries(prices)) {
                        if (symbol.includes('BTC') || symbol.includes('ETH') || symbol.includes('BNB') || symbol.includes('SOL')) {
                            const priceDisplay = parseFloat(price).toFixed(2);
                            
                            // 視情況更新資產1或資產2的價格
                            if ($('#asset1Price').parent().find('.card-title').text() === symbol) {
                                $('#asset1Price').text(priceDisplay);
                            }
                            
                            if ($('#asset2Price').parent().find('.card-title').text() === symbol) {
                                $('#asset2Price').text(priceDisplay);
                            }
                        }
                    }
                }
            }
        });
        
        // 獲取持倉數據
        $.ajax({
            url: '/api/market/positions',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success') {
                    const positionsTableBody = $('#positionsTableBody');
                    positionsTableBody.empty();
                    
                    if (response.positions && response.positions.length > 0) {
                        // 過濾出有持倉的數據
                        const activePositions = response.positions.filter(p => 
                            parseFloat(p.positionAmt) !== 0);
                        
                        if (activePositions.length > 0) {
                            activePositions.forEach(function(position) {
                                const row = $('<tr></tr>');
                                row.append($('<td></td>').text(position.symbol));
                                row.append($('<td></td>').text(parseFloat(position.positionAmt) > 0 ? '多' : '空'));
                                row.append($('<td></td>').text(Math.abs(position.positionAmt)));
                                
                                // 根據盈虧添加顏色
                                const profitTd = $('<td></td>').text(parseFloat(position.unrealizedProfit).toFixed(2));
                                if (parseFloat(position.unrealizedProfit) > 0) {
                                    profitTd.addClass('text-success');
                                } else if (parseFloat(position.unrealizedProfit) < 0) {
                                    profitTd.addClass('text-danger');
                                }
                                row.append(profitTd);
                                
                                positionsTableBody.append(row);
                            });
                        } else {
                            positionsTableBody.append('<tr><td colspan="4">暫無持倉</td></tr>');
                        }
                    } else {
                        positionsTableBody.append('<tr><td colspan="4">暫無持倉</td></tr>');
                    }
                }
            }
        });
    }
    // 刷新圖表頁
    else if (activeTab === '#charts') {
        // 獲取Z分數圖表數據
        $.ajax({
            url: '/api/charts/z-score',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success') {
                    // 設置ASCII圖表文本
                    $('#zScoreText').text(response.chart || '暫無數據');
                    
                    // 更新Chart.js圖表
                    try {
                        if (response.chartData) {
                            const chartData = JSON.parse(response.chartData);
                            // 檢查圖表實例是否存在
                            if (!zScoreChart) {
                                initCharts(); // 如果不存在則初始化
                            } else {
                                updateZScoreChart(chartData); // 更新現有圖表
                            }
                        }
                    } catch (e) {
                        console.error('解析Z分數圖表數據失敗', e);
                    }
                }
            }
        });
        
        // 獲取價差圖表數據
        $.ajax({
            url: '/api/charts/spread',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success') {
                    // 設置ASCII圖表文本
                    $('#spreadText').text(response.chart || '暫無數據');
                    
                    // 更新Chart.js圖表
                    try {
                        if (response.chartData) {
                            const chartData = JSON.parse(response.chartData);
                            // 檢查圖表實例是否存在
                            if (!spreadChart) {
                                initCharts(); // 如果不存在則初始化
                            } else {
                                updateSpreadChart(chartData); // 更新現有圖表
                            }
                        }
                    } catch (e) {
                        console.error('解析價差圖表數據失敗', e);
                    }
                }
            }
        });
    }
    // 刷新日誌頁
    else if (activeTab === '#logs') {
        $.ajax({
            url: '/api/logs/system',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success') {
                    const logsContainer = $('#systemLogs');
                    logsContainer.empty();
                    
                    if (response.logs && response.logs.length > 0) {
                        response.logs.forEach(function(log) {
                            const logLine = $('<div></div>').text(log);
                            
                            // 根據日誌級別添加顏色
                            if (log.includes('[ERROR]')) {
                                logLine.addClass('text-danger');
                            } else if (log.includes('[WARN]')) {
                                logLine.addClass('text-warning');
                            } else if (log.includes('[INFO]')) {
                                logLine.addClass('text-info');
                            }
                            
                            logsContainer.append(logLine);
                        });
                    } else {
                        logsContainer.text('暫無日誌');
                    }
                    
                    // 滾動到底部
                    logsContainer.scrollTop(logsContainer.prop('scrollHeight'));
                }
            }
        });
    }
    // 刷新交易操作日誌頁
    else if (activeTab === '#trade-logs') {
        $.ajax({
            url: '/api/logs/trade',
            type: 'GET',
            success: function(response) {
                if (response.status === 'success') {
                    const logsContainer = $('#tradeLogs');
                    logsContainer.empty();
                    
                    if (response.logs && response.logs.length > 0) {
                        response.logs.forEach(function(log) {
                            const logLine = $('<div></div>').text(log);
                            
                            // 根據日誌級別添加顏色
                            if (log.includes('[ERROR]')) {
                                logLine.addClass('text-danger');
                            } else if (log.includes('[WARN]')) {
                                logLine.addClass('text-warning');
                            } else if (log.includes('[INFO]')) {
                                logLine.addClass('text-info');
                            }
                            
                            logsContainer.append(logLine);
                        });
                    } else {
                        logsContainer.text('暫無交易操作日誌');
                    }
                    
                    // 滾動到底部
                    logsContainer.scrollTop(logsContainer.prop('scrollHeight'));
                }
            }
        });
    }
}

/**
 * 刷新交易操作日誌
 */
function refreshTradeLogs() {
    // 重新啟動倒數計時
    startCountdownTimer();
    
    $.ajax({
        url: '/api/logs/trade',
        type: 'GET',
        success: function(response) {
            if (response.status === 'success') {
                const logsContainer = $('#tradeLogs');
                logsContainer.empty();
                
                if (response.logs && response.logs.length > 0) {
                    response.logs.forEach(function(log) {
                        const logLine = $('<div></div>').text(log);
                        
                        // 根據日誌級別添加顏色
                        if (log.includes('[ERROR]')) {
                            logLine.addClass('text-danger');
                        } else if (log.includes('[WARN]')) {
                            logLine.addClass('text-warning');
                        } else if (log.includes('[INFO]')) {
                            logLine.addClass('text-info');
                        }
                        
                        logsContainer.append(logLine);
                    });
                } else {
                    logsContainer.text('暫無交易操作日誌');
                }
                
                // 滾動到底部
                logsContainer.scrollTop(logsContainer.prop('scrollHeight'));
            }
        }
    });
}

/**
 * 顯示提示框
 * 
 * @param {string} title 標題
 * @param {string} message 訊息內容
 * @param {boolean} isError 是否為錯誤提示
 */
function showToast(title, message, isError = false) {
    const toast = $('#resultToast');
    const toastTitle = $('#toastTitle');
    const toastMessage = $('#toastMessage');
    
    toastTitle.text(title);
    toastMessage.text(message);
    
    // 添加錯誤樣式
    if (isError) {
        toast.addClass('bg-danger text-white');
    } else {
        toast.removeClass('bg-danger text-white');
    }
    
    // 顯示提示框
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
}