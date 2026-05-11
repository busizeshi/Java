package com.jwd.skill;

import com.jwd.handler.SkillHandler;
import com.jwd.model.server.Artifact;
import com.jwd.model.server.Message;
import com.jwd.model.server.Part;
import com.jwd.model.server.Task;
import com.jwd.model.server.TaskState;
import com.jwd.model.server.TaskStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReportGenerationSkillHandler implements SkillHandler {

    private static final Pattern FULL_RANGE = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日到(\\d{4})年(\\d{1,2})月(\\d{1,2})日");
    private static final Pattern SAME_YEAR_RANGE = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日到(\\d{1,2})月(\\d{1,2})日");

    private final ChatClient chatClient;
    private final Map<String, PendingTaskContext> pendingTasks = new ConcurrentHashMap<>();

    public ReportGenerationSkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a sales analyst. Generate concise, structured sales reports.")
                .build();
    }

    @Override
    public String skillId() {
        return "generate-sales-report";
    }

    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        AnalysisParams params = extractParams(history);
        if (params.startDate() == null || params.endDate() == null) {
            pendingTasks.put(taskId, new PendingTaskContext(sessionId, history, params));
            return buildInputRequiredTask(taskId, sessionId, history, params);
        }
        pendingTasks.remove(taskId);
        return generateReport(taskId, sessionId, history, params);
    }

    public Task continueTask(String taskId, String sessionId, List<Message> updatedHistory) {
        pendingTasks.remove(taskId);
        AnalysisParams params = extractParams(updatedHistory);
        if (params.startDate() == null || params.endDate() == null) {
            pendingTasks.put(taskId, new PendingTaskContext(sessionId, updatedHistory, params));
            return buildInputRequiredTask(taskId, sessionId, updatedHistory, params);
        }
        return generateReport(taskId, sessionId, updatedHistory, params);
    }

    private Task buildInputRequiredTask(String taskId, String sessionId, List<Message> history, AnalysisParams params) {
        String question = buildClarifyingQuestion(params);
        return new Task(
                taskId,
                sessionId,
                new TaskStatus(
                        TaskState.INPUT_REQUIRED,
                        new Message("agent", List.of(Part.text(question))),
                        Instant.now().toString()
                ),
                history,
                List.of(),
                Map.of()
        );
    }

    private Task generateReport(String taskId, String sessionId, List<Message> history, AnalysisParams params) {
        String region = params.region() == null ? "全国" : params.region();
        String prompt = String.format(
                "Generate a sales report in Chinese. Start date: %s. End date: %s. Region: %s. Include summary, trend, anomaly hints and action suggestions.",
                params.startDate(),
                params.endDate(),
                region
        );

        String report = chatClient.prompt().user(prompt).call().content();
        if (report == null || report.isBlank()) {
            report = "## 销售报告\n\n未生成到有效内容，请稍后重试。";
        }

        return new Task(
                taskId,
                sessionId,
                new TaskStatus(
                        TaskState.COMPLETED,
                        new Message("agent", List.of(Part.text(report))),
                        Instant.now().toString()
                ),
                history,
                List.of(new Artifact("销售报告", "根据补充信息生成的销售分析报告", List.of(Part.text(report)))),
                Map.of(
                        "startDate", params.startDate(),
                        "endDate", params.endDate(),
                        "region", region
                )
        );
    }

    private AnalysisParams extractParams(List<Message> history) {
        String allUserText = history.stream()
                .filter(m -> "user".equals(m.role()))
                .flatMap(m -> m.parts().stream())
                .filter(p -> "text".equals(p.type()) && p.text() != null)
                .map(Part::text)
                .reduce("", (a, b) -> a + "\n" + b);

        DateRange dateRange = parseDateRange(allUserText);
        String region = extractRegion(allUserText);
        return new AnalysisParams(dateRange.startDate(), dateRange.endDate(), region);
    }

    private DateRange parseDateRange(String text) {
        Matcher full = FULL_RANGE.matcher(text);
        if (full.find()) {
            String start = formatDate(full.group(1), full.group(2), full.group(3));
            String end = formatDate(full.group(4), full.group(5), full.group(6));
            return new DateRange(start, end);
        }

        Matcher sameYear = SAME_YEAR_RANGE.matcher(text);
        if (sameYear.find()) {
            String year = sameYear.group(1);
            String start = formatDate(year, sameYear.group(2), sameYear.group(3));
            String end = formatDate(year, sameYear.group(4), sameYear.group(5));
            return new DateRange(start, end);
        }

        return new DateRange(null, null);
    }

    private String formatDate(String year, String month, String day) {
        return String.format("%s-%02d-%02d", year, Integer.parseInt(month), Integer.parseInt(day));
    }

    private String extractRegion(String text) {
        if (text.contains("华东")) return "华东";
        if (text.contains("华南")) return "华南";
        if (text.contains("华北")) return "华北";
        if (text.contains("华中")) return "华中";
        if (text.contains("西南")) return "西南";
        if (text.contains("西北")) return "西北";
        if (text.contains("东北")) return "东北";
        if (text.contains("全国")) return "全国";
        return null;
    }

    private String buildClarifyingQuestion(AnalysisParams params) {
        List<String> missing = new ArrayList<>();
        if (params.startDate() == null) missing.add("开始日期");
        if (params.endDate() == null) missing.add("结束日期");
        if (params.region() == null) missing.add("地区（可选）");

        return "需要补充以下信息才能生成报告：\n"
                + String.join("、", missing)
                + "\n\n请提供这些信息，例如：2025年1月1日到2025年1月31日，华东地区";
    }

    record AnalysisParams(String startDate, String endDate, String region) {}

    record DateRange(String startDate, String endDate) {}

    record PendingTaskContext(String sessionId, List<Message> history, AnalysisParams partialParams) {}
}
