package com.ssafy.domain.news.job;

import com.ssafy.domain.news.service.NewsScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScrapingJob implements Job {

    private final NewsScraperService newsScraperService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("========================================");
        log.info("뉴스 자동 크롤링 Job 시작");
        log.info("실행 시간: {}", java.time.LocalDateTime.now());
        log.info("========================================");

        try {
            // Google News IT 뉴스 크롤링 (작동 확인됨!)
            newsScraperService.scrapeGoogleNewsIT();

            log.info("========================================");
            log.info("뉴스 자동 크롤링 Job 완료");
            log.info("========================================");

        } catch (Exception e) {
            log.error("뉴스 자동 크롤링 Job 실패", e);
            throw new JobExecutionException("뉴스 크롤링 중 오류 발생", e);
        }
    }
}