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
        try {
            newsScraperService.scrapeGoogleNewsIT();

} catch (Exception e) {
    throw new JobExecutionException("뉴스 크롤링 중 오류 발생", e);
        }
    }
}

