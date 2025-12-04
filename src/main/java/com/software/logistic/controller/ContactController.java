package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.Contact;
import com.software.logistic.repository.ContactRepository;
import com.software.logistic.service.DingTalkRobotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DingTalkRobotService dingTalkRobotService;

    /**
     * 处理在线咨询表单提交
     * @param contact 联系信息
     * @return 响应结果
     */
    @PostMapping("/contact")
    public ResponseResult<?> submitContact(@RequestBody Contact contact) {
        try {
            // 保存联系信息到数据库
            contactRepository.save(contact);
            
            // 发送钉钉群聊通知
            String message = "\n"+"姓名：" + contact.getName() + "\n" +
                            "邮箱：" + contact.getEmail() + "\n" +
                            "留言内容：\n" + contact.getMessage();
            dingTalkRobotService.sendTextMessage(message);
            
            return ResponseResult.success("留言提交成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error("留言提交失败：" + e.getMessage());
        }
    }
}