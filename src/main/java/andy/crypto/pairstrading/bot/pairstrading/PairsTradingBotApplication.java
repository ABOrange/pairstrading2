package andy.crypto.pairstrading.bot.pairstrading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = {
        "andy.crypto.pairstrading.bot.pairstrading", 
        "andy.crypto.pairstrading.bot.config",
        "andy.crypto.pairstrading.bot.service",
        "andy.crypto.pairstrading.bot.controller",
        "andy.crypto.pairstrading.bot.entity",
        "andy.crypto.pairstrading.bot.repository",
        "andy.crypto.pairstrading.bot.util"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = {
            ".*\\.pairstrading\\.controller\\.GlobalExceptionHandler",
            ".*\\.pairstrading\\.controller\\.HomeController"
        }
    )
)
@EntityScan(basePackages = {
    "andy.crypto.pairstrading.bot.entity", 
    "andy.crypto.pairstrading.bot.pairstrading.model"
})
@EnableJpaRepositories(basePackages = {
    "andy.crypto.pairstrading.bot.repository", 
    "andy.crypto.pairstrading.bot.pairstrading.repository"
})
public class PairsTradingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(PairsTradingBotApplication.class, args);
    }
}
