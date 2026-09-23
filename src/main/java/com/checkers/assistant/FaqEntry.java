package com.checkers.assistant;

import java.util.List;

public record FaqEntry(String id, String question, String answer, List<String> keywords) {
}
