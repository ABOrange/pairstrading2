/**
 * 配對交易機器人儀表板 - 共用功能
 */

/**
 * 顯示提示框
 * 
 * @param {string} type 提示類型 (success, error, warning, info)
 * @param {string} message 訊息內容
 */
function showToast(type, message) {
    const toast = $('#resultToast');
    const toastTitle = $('#toastTitle');
    const toastMessage = $('#toastMessage');
    
    // 根據類型設置不同的標題和樣式
    let title = '';
    
    // 先清除所有可能的樣式
    toast.removeClass('bg-success bg-danger bg-warning bg-info text-white');
    
    // 根據類型設置樣式
    switch (type) {
        case 'success':
            title = '成功';
            toast.addClass('bg-success text-white');
            break;
        case 'error':
            title = '錯誤';
            toast.addClass('bg-danger text-white');
            break;
        case 'warning':
            title = '警告';
            toast.addClass('bg-warning');
            break;
        case 'info':
            title = '提示';
            toast.addClass('bg-info text-white');
            break;
        default:
            title = '提示';
    }
    
    toastTitle.text(title);
    toastMessage.text(message);
    
    // 顯示提示框
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
}

/**
 * 隱藏提示框
 */
function hideToast() {
    const toast = $('#resultToast');
    const bsToast = bootstrap.Toast.getInstance(toast);
    if (bsToast) {
        bsToast.hide();
    }
}

/**
 * 頁面刷新函數 - 由各頁面實現
 * 此為預設實現，會重新載入整個頁面
 */
function refreshPage() {
    location.reload();
}

/**
 * 重置倒數計時器
 */
function resetRefreshCounter() {
    counter = 20;
    $('#refreshCounter').text(counter);
}

/**
 * 手動執行任務的AJAX呼叫
 * 不會導致頁面跳轉
 */
function runJobManually() {
    // 顯示加載中的提示
    showToast('info', '正在執行任務，請稍候...');
    
    // 使用AJAX呼叫執行任務API
    $.ajax({
        url: '/api/job/run-manually',
        type: 'GET',
        dataType: 'json',
        success: function(response) {
            // 顯示成功消息
            showToast('success', '任務已成功執行');
            
            // // 如果當前頁面有自定義的頁面刷新函數，則執行
            // if (typeof refreshPage === 'function') {
            //     // 等待2秒後刷新頁面數據
            //     setTimeout(function() {
            //         refreshPage();
            //     }, 2000);
            // }
        },
        error: function(xhr) {
            // 處理錯誤
            // let errorMsg = '執行任務失敗';
            // if (xhr.responseJSON && xhr.responseJSON.message) {
            //     errorMsg += ': ' + xhr.responseJSON.message;
            // }
            // showToast('error', errorMsg);
        }
    });
    
    // 防止默認的連結行為
    return false;
}
