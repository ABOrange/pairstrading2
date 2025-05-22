package andy.crypto.pairstrading.bot.pairstrading.runner;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuartzJobRunner implements ApplicationRunner {

    @Autowired
    private Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 確保應用啟動時排程器也啟動
            if (!scheduler.isStarted()) {
                scheduler.start();
                log.info("Quartz排程器已啟動");
            }
        } catch (SchedulerException e) {
            log.error("啟動Quartz排程器失敗", e);
            throw e;
        }
    }
}
