package com.smartcollab.local.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC 관련 설정을 담당합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 특정 URL 경로를 특정 View(HTML 파일)로 직접 연결합니다.
     * /share/ 로 시작하는 모든 주소로 접속 시, share.html 파일을 보여주도록 설정합니다.
     *
     * @param registry ViewController 등록기
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // "/share/**" 경로로 들어오는 모든 요청을 "/share.html"로 포워딩합니다.
        registry.addViewController("/share/**").setViewName("forward:/share.html");
    }
}