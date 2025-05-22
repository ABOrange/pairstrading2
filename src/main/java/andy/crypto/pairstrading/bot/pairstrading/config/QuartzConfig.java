package andy.crypto.pairstrading.bot.pairstrading.config;

import andy.crypto.pairstrading.bot.pairstrading.job.PairsTradingJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    private static final String PAIRS_TRADING_JOB_IDENTITY = "pairsTradingJob";
    private static final String PAIRS_TRADING_TRIGGER = "pairsTradingTrigger";
    private static final String PAIRS_TRADING_GROUP = "pairsTradingGroup";

    @Bean
    public JobDetail pairsTradingJobDetail() {
        return JobBuilder.newJob(PairsTradingJob.class)
                .withIdentity(PAIRS_TRADING_JOB_IDENTITY, PAIRS_TRADING_GROUP)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger pairsTradingJobTrigger() {
        // 設定為每5分鐘執行一次
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
                .simpleSchedule()
//                .withIntervalInSeconds(20)
                .withIntervalInMinutes(10)
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(pairsTradingJobDetail())
                .withIdentity(PAIRS_TRADING_TRIGGER, PAIRS_TRADING_GROUP)
                .withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * 如果需要基於Cron表達式的排程，可以使用以下方法
     */
    /*
    @Bean
    public Trigger pairsTradingCronJobTrigger() {
        // 設定為每天上午9點到下午4點，每5分鐘執行一次
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule("0 0/5 9-16 * * ?");

        return TriggerBuilder.newTrigger()
                .forJob(pairsTradingJobDetail())
                .withIdentity("pairsTradingCronTrigger", PAIRS_TRADING_GROUP)
                .withSchedule(scheduleBuilder)
                .build();
    }
    */
}
