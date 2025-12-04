package com.software.logistic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DingTalkRobotService {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkRobotService.class);

    @Value("${dingtalk.robot.webhook}")
    private String webhookUrl;

    /**
     * 发送文本消息到钉钉群
     * @param message 消息内容
     */
    public void sendTextMessage(String message) {
        logger.info("开始发送钉钉机器人通知，webhook: {}", webhookUrl);
        logger.info("消息内容: {}", message);
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            // 构建请求体，使用text格式发送完整的用户留言内容
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("msgtype", "text");
            
            Map<String, String> textContent = new HashMap<>();
            // 发送完整的用户留言内容，确保关键词"用户留言"在消息开头
            String fullMessage = "用户留言：" + message;
            textContent.put("content", fullMessage);
            requestBody.put("text", textContent);
            
            logger.info("发送请求，请求体: {}", requestBody);
            
            // 发送请求
            String response = restTemplate.postForObject(webhookUrl, requestBody, String.class);
            logger.info("钉钉机器人通知发送成功，响应: {}", response);
            logger.info("webhook URL: {}", webhookUrl);
            logger.info("请求体: {}", requestBody);
        } catch (Exception e) {
            logger.error("钉钉机器人通知发送失败: {}", e.getMessage(), e);
        }
    }
}