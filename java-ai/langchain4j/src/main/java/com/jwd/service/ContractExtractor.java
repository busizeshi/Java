package com.jwd.service;

import com.jwd.model.ContractInfo;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "qwenTurboModel")
public interface ContractExtractor {

    @SystemMessage("""
            你是合同信息提取专家。
            只提取文本中明确表述的信息，不推断不猜测。
            日期统一转为 YYYY-MM-DD 格式。
            无法确定的字段填 null，列表无内容填空列表。
            """)
    ContractInfo extractContractInfo(String text);
}
