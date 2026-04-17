package com.jwd.service;

import com.jwd.model.TicketCategory;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "qwenTurboModel")
public interface TicketClassifier {

    @SystemMessage("""
            对客户工单进行分类。
            BILLING：账单/付款问题
            TECH_SUPPORT：技术故障
            FEATURE_REQUEST：功能建议
            ACCOUNT：账号/权限问题
            OTHER：其他
            """)
    TicketCategory classify(String ticket);

}
