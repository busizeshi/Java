package com.jwd.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReactTools {

    private final Map<String, BigDecimal> inventory = new ConcurrentHashMap<>();

    public ReactTools() {
        inventory.put("Java并发编程实战", BigDecimal.valueOf(88));
        inventory.put("Spring实战", BigDecimal.valueOf(99));
        inventory.put("LangChain4j实战", BigDecimal.valueOf(129));
    }

    @Tool("根据商品名称查询商品单价，返回元")
    public String queryPrice(@P("商品名") String productName) {
        BigDecimal price = inventory.get(productName);
        if (price == null) {
            return "NOT_FOUND";
        }
        return productName + "单价为" + price.stripTrailingZeros().toPlainString() + "元";
    }

    @Tool("计算折扣价，discountPercent为0-100")
    public String discountPrice(@P("原价") double originPrice, @P("折扣百分比") double discountPercent) {
        BigDecimal price = BigDecimal.valueOf(originPrice);
        BigDecimal discount = BigDecimal.valueOf(discountPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal result = price.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
        return "折后价为" + result.toPlainString() + "元";
    }

    @Tool("计算满减价格，参数为原价、满多少、减多少")
    public String applyFullReduction(@P("原价") double originPrice, @P("门槛") double threshold, @P("减免") double reduction) {
        BigDecimal price = BigDecimal.valueOf(originPrice);
        BigDecimal gate = BigDecimal.valueOf(threshold);
        BigDecimal cut = BigDecimal.valueOf(reduction);
        if (price.compareTo(gate) >= 0) {
            BigDecimal result = price.subtract(cut).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            return "满足满减，最终价" + result.toPlainString() + "元";
        }
        return "不满足满减条件，最终价" + price.setScale(2, RoundingMode.HALF_UP).toPlainString() + "元";
    }

    @Tool("返回今天日期，格式yyyy-MM-dd")
    public String today() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Tool("给出学习建议，topic可选: react/tool/memory/prompt")
    public String learningTips(@P("学习主题") String topic) {
        return switch (topic.toLowerCase()) {
            case "react" -> "先理解Thought-Action-Observation循环，再看停止条件和容错策略。";
            case "tool" -> "工具参数要结构化，返回值建议短句+关键字段，避免歧义。";
            case "memory" -> "区分短期会话记忆与长期知识库存储，先控制窗口大小再考虑持久化。";
            case "prompt" -> "系统提示词应定义：角色、工具使用规则、输出格式、失败回退。";
            default -> "建议从一个可解释的端到端案例入手，再逐步扩展工具和记忆。";
        };
    }
}
