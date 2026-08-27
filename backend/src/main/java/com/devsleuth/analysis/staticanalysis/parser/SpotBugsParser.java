package com.devsleuth.analysis.staticanalysis.parser;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.RawFinding;
import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.*;

/**
 * Parses SpotBugs XML output into RawFindings.
 *
 * SpotBugs XML structure:
 * <BugCollection>
 *   <BugInstance type="..." priority="..." category="...">
 *     <LongMessage>...</LongMessage>
 *     <SourceLine classname="..." start="..." end="..." sourcepath="..."/>
 *   </BugInstance>
 * </BugCollection>
 */
public class SpotBugsParser {

    public List<RawFinding> parse(String xmlOutput, AnalysisInput input) {
        List<RawFinding> findings = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlOutput.getBytes(StandardCharsets.UTF_8)));

            NodeList bugs = doc.getElementsByTagName("BugInstance");
            for (int i = 0; i < bugs.getLength(); i++) {
                Element bug = (Element) bugs.item(i);
                String type = bug.getAttribute("type");
                int priority = Integer.parseInt(bug.getAttribute("priority"));
                String category = bug.getAttribute("category");

                String message = "";
                NodeList msgs = bug.getElementsByTagName("LongMessage");
                if (msgs.getLength() > 0) {
                    message = msgs.item(0).getTextContent();
                }

                String filePath = "";
                int lineStart = 0;
                int lineEnd = 0;
                NodeList sources = bug.getElementsByTagName("SourceLine");
                if (sources.getLength() > 0) {
                    Element src = (Element) sources.item(0);
                    filePath = src.getAttribute("sourcepath");
                    lineStart = parseIntSafe(src.getAttribute("start"));
                    lineEnd = parseIntSafe(src.getAttribute("end"));
                }

                findings.add(new RawFinding(
                        FindingSource.STATIC,
                        mapCategory(category),
                        mapPriority(priority),
                        100,
                        type,
                        message,
                        "Fix SpotBugs issue: " + type,
                        resolveFilePath(filePath, input),
                        lineStart,
                        lineEnd
                ));
            }
        } catch (Exception e) {
            // Malformed XML: return empty
        }
        return findings;
    }

    private Severity mapPriority(int priority) {
        return switch (priority) {
            case 1 -> Severity.HIGH;
            case 2 -> Severity.MEDIUM;
            case 3 -> Severity.LOW;
            default -> Severity.INFO;
        };
    }

    private FindingCategory mapCategory(String category) {
        if (category == null) return FindingCategory.BUG;
        return switch (category) {
            case "SECURITY" -> FindingCategory.SECURITY;
            case "PERFORMANCE" -> FindingCategory.PERFORMANCE;
            case "STYLE", "I18N" -> FindingCategory.QUALITY;
            default -> FindingCategory.BUG;
        };
    }

    private String resolveFilePath(String sourcePath, AnalysisInput input) {
        for (var file : input.files()) {
            if (file.filePath().endsWith(sourcePath) || sourcePath.endsWith(file.filePath())) {
                return file.filePath();
            }
        }
        return sourcePath;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
