package com.ssafy.config;

import com.ssafy.domain.news.job.NewsScrapingJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    /**
     * 뉴스 크롤링 Job 정의
     */
    @Bean
    public JobDetail newsScrapingJobDetail() {
        return JobBuilder.newJob(NewsScrapingJob.class)
                .withIdentity("newsScrapingJob", "news-jobs")
                .withDescription("IT 뉴스 자동 크롤링 작업")
                .storeDurably()
                .build();
    }

    /**
     * 뉴스 크롤링 Trigger - 매일 오전 6시 실행
     */
    @Bean
    public Trigger newsScrapingTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(newsScrapingJobDetail())
                .withIdentity("newsScrapingTrigger", "news-triggers")
                .withDescription("매일 오전 6시 뉴스 크롤링")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule("0 0 6 * * ?")
                                .inTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"))
                )
                .build();
    }
}