package com.devsleuth.analysis.ai;

import com.devsleuth.analysis.ai.model.ReviewContext;
import com.devsleuth.analysis.ai.model.ReviewContext.FileContext;
import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds token-efficient review context from AnalysisInput.
 * Reduces full file content to only the changed lines + surrounding context.
 */
@Component
public class ReviewContextBuilder {

    // ponytail: max lines of surrounding context per changed region; keeps token usage reasonable
    private static final int CONTEXT_LINES = 20;
    private static final int MAX_FILE_CONTENT_CHARS = 8000;

    public ReviewContext build(AnalysisInput input) {
        List<FileContext> fileContexts = new ArrayList<>();

        for (FileChange file : input.files()) {
            String surroundingCode = buildSurroundingCode(file);
            fileContexts.add(new FileContext(
                    file.filePath(),
                    file.patch(),
                    surroundingCode
            ));
        }

        return new ReviewContext(
                input.repositoryFullName(),
                "", // PR title not in AnalysisInput; ponytail: add if needed
                input.commitSha(),
                fileContexts
        );
    }

    /**
     * Extracts the relevant surrounding code context.
     * If full content is small enough, include it. Otherwise, extract around changed lines.
     */
    private String buildSurroundingCode(FileChange file) {
        if (file.fullContent() == null) {
            return null;
        }

        // If the file is small, just send it all
        if (file.fullContent().length() <= MAX_FILE_CONTENT_CHARS) {
            return file.fullContent();
        }

        // Extract changed line numbers from patch and include surrounding context
        if (file.patch() == null) {
            return truncate(file.fullContent());
        }

        String[] lines = file.fullContent().split("\n");
        List<Integer> changedLines = extractChangedLineNumbers(file.patch());

        if (changedLines.isEmpty()) {
            return truncate(file.fullContent());
        }

        StringBuilder sb = new StringBuilder();
        for (int lineNum : changedLines) {
            int start = Math.max(0, lineNum - CONTEXT_LINES - 1);
            int end = Math.min(lines.length, lineNum + CONTEXT_LINES);
            for (int i = start; i < end; i++) {
                sb.append(lines[i]).append('\n');
            }
            sb.append("// ... \n");
        }
        return truncate(sb.toString());
    }

    /**
     * Parses unified diff hunk headers (@@ -a,b +c,d @@) to find changed line numbers.
     */
    private List<Integer> extractChangedLineNumbers(String patch) {
        List<Integer> lines = new ArrayList<>();
        int currentLine = 0;
        for (String line : patch.split("\n")) {
            if (line.startsWith("@@")) {
                // Parse +start from @@ -X,Y +Z,W @@
                int plusIdx = line.indexOf('+', 2);
                if (plusIdx >= 0) {
                    String after = line.substring(plusIdx + 1);
                    int commaIdx = after.indexOf(',');
                    int spaceIdx = after.indexOf(' ');
                    int endIdx = commaIdx > 0 ? commaIdx : (spaceIdx > 0 ? spaceIdx : after.length());
                    try {
                        currentLine = Integer.parseInt(after.substring(0, endIdx));
                        lines.add(currentLine);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                lines.add(currentLine);
                currentLine++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                // deleted lines don't advance the new-file line counter
            } else {
                currentLine++;
            }
        }
        return lines.stream().distinct().toList();
    }

    private String truncate(String s) {
        if (s.length() <= MAX_FILE_CONTENT_CHARS) return s;
        return s.substring(0, MAX_FILE_CONTENT_CHARS) + "\n// ... (truncated)";
    }
}
