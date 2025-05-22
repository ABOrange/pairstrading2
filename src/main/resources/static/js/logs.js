/**
 * 配對交易機器人儀表板 - 日誌頁面 JavaScript
 */

// 頁面載入完成後執行初始化
$(document).ready(function() {
    // 初始化事件監聽
    initEventListeners();
    
    // 初始加載數據
    refreshPage();
});

/**
 * 初始化事件監聽
 */
function initEventListeners() {
    // 系統日誌頁面
    if ($('#refreshLogsBtn').length > 0) {
        // 刷新系統日誌
        $('#refreshLogsBtn').on('click', function() {
            refreshLogs();
            resetRefreshCounter();
        });
        
        // 清除日誌
        $('#clearLogsBtn').on('click', function() {
            if (confirm('確定要清除所有日誌嗎？')) {
                $.ajax({
                    url: '/api/logs/clear',
                    type: 'GET',
                    success: function(response) {
                        if (response.status === 'success') {
                            showToast('成功', response.message);
                            refreshLogs();
                        } else {
                            showToast('錯誤', response.message, true);
                        }
                    },
                    error: function(error) {
                        showToast('錯誤', '清除日誌失敗：' + error.statusText, true);
                    }
                });
            }
        });
    }
    
    // 交易操作日誌頁面
    if ($('#refreshTradeLogsBtn').length > 0) {
        // 刷新交易日誌
        $('#refreshTradeLogsBtn').on('click', function() {
            refreshTradeLogs();
            resetRefreshCounter();
        });
    }
}

/**
 * 刷新頁面數據 (覆蓋全局 refreshPage 函數)
 */
function refreshPage() {
    // 根據頁面類型刷新不同的數據
    if ($('#systemLogs').length > 0) {
        refreshLogs();
    }
    
    if ($('#tradeLogs').length > 0) {
        refreshTradeLogs();
    }
}

/**
 * 刷新系統日誌
 */
function refreshLogs() {
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
 * 刷新交易操作日誌
 */
function refreshTradeLogs() {
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