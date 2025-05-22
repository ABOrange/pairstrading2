/**
 * 修復點擊事件處理器，確保使用 runJobManually 函數
 * 
 * 這個檔案用於解決 #runJobBtn 按鈕調用不同 API 的問題
 */
$(document).ready(function() {
    // 移除所有綁定到 #runJobBtn 的點擊事件
    $('#runJobBtn').off('click');
    
    // 重新綁定使用 runJobManually 函數
    $('#runJobBtn').on('click', function(e) {
        e.preventDefault();
        return runJobManually();
    });
});