package andy.crypto.pairstrading.bot.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 請求工具類
 * 用於在Thymeleaf模板中使用，提供當前請求的相關信息
 */
@Component("requestUtils")
public class RequestUtils {

    /**
     * 檢查當前請求URI是否為指定路徑
     * 
     * @param path 指定路徑
     * @return 如果當前請求URI與指定路徑相同，則返回true
     */
    public boolean isCurrentPage(String path) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }
        return request.getRequestURI().equals(path);
    }
    
    /**
     * 檢查當前請求URI是否以指定字串開頭
     * 
     * @param prefix 前綴字串
     * @return 如果當前請求URI以指定字串開頭，則返回true
     */
    public boolean urlStartsWith(String prefix) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }
        return request.getRequestURI().startsWith(prefix);
    }
    
    /**
     * 獲取當前請求對象
     * 
     * @return 當前HttpServletRequest，如果無法獲取則返回null
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest();
            }
        } catch (Exception e) {
            // 忽略異常
        }
        return null;
    }
}
