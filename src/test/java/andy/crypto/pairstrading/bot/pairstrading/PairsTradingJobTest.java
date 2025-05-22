package andy.crypto.pairstrading.bot.pairstrading;

import andy.crypto.pairstrading.bot.pairstrading.job.PairsTradingJob;
import andy.crypto.pairstrading.bot.pairstrading.service.PairsTradingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PairsTradingJobTest {

//    @Mock
//    private PairsTradingService pairsTradingService;
//
//    @Mock
//    private JobExecutionContext jobExecutionContext;
//
//    @InjectMocks
//    private PairsTradingJob pairsTradingJob;
//
//    @Test
//    void testExecuteInternal() throws JobExecutionException {
//        // 設置服務層方法的返回值
//        Mockito.when(pairsTradingService.calculateSignal()).thenReturn(true);
//
//        // 執行任務
//        pairsTradingJob.executeInternal(jobExecutionContext);
//
//        // 驗證服務方法被調用
//        Mockito.verify(pairsTradingService).fetchMarketData();
//        Mockito.verify(pairsTradingService).analyzeCorrelation();
//        Mockito.verify(pairsTradingService).calculateSignal();
//        Mockito.verify(pairsTradingService).executeTrade(true);
//    }
//
//    @Test
//    void testNoSignal() throws JobExecutionException {
//        // 設置服務層方法的返回值為false（無交易信號）
//        Mockito.when(pairsTradingService.calculateSignal()).thenReturn(false);
//
//        // 執行任務
//        pairsTradingJob.executeInternal(jobExecutionContext);
//
//        // 驗證服務方法被調用，但executeTrade不應該被調用
//        Mockito.verify(pairsTradingService).fetchMarketData();
//        Mockito.verify(pairsTradingService).analyzeCorrelation();
//        Mockito.verify(pairsTradingService).calculateSignal();
//        Mockito.verify(pairsTradingService, Mockito.never()).executeTrade(Mockito.anyBoolean());
//    }
}
