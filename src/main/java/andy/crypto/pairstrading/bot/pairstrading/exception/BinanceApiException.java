package andy.crypto.pairstrading.bot.pairstrading.exception;

/**
 * 幣安API異常類
 */
public class BinanceApiException extends RuntimeException {

    public BinanceApiException(String message) {
        super(message);
    }

    public BinanceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
