package com.jwd.model;

import java.util.List;

public record SentimentResult(String sentiment, List<String> reasons, int score) {
}
