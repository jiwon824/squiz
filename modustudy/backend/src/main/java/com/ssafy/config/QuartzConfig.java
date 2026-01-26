package com.ssafy.config;

import com.ssafy.domain.news.job.NewsScrapingJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail newsScrapingJobDetail() {
        return JobBuilder.newJob(NewsScrapingJob.class)
                .withIdentity("newsScrapingJob", "news-jobs")
                .withDescription("IT 뉴스 자동 크롤링 작업")
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger newsScrapingTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(newsScrapingJobDetail())
                .withIdentity("newsScrapingTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))  // 10분마다 (개발용)
                .build();
    }
}