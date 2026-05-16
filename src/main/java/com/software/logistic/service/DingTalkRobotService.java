package com.software.logistic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DingTalkRobotService {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkRobotService.class);

    @Value("${dingtalk.robot.webhook:}")
    private String webhookUrl;

    public void sendTextMessage(String message) {
        if (!StringUtils.hasText(webhookUrl)) {
            logger.warn("DingTalk webhook is not configured; skipping notification");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "text");

            Map<String, String> textContent = new HashMap<>();
            textContent.put("content", "用户留言：" + message);
            requestBody.put("text", textContent);

            String response = restTemplate.postForObject(webhookUrl, requestBody, String.class);
            logger.info("DingTalk notification sent, response: {}", response);
        } catch (Exception e) {
            logger.error("DingTalk notification failed: {}", e.getMessage(), e);
        }
    }
}
