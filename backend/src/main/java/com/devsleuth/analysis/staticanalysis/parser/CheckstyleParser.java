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
 * Parses Checkstyle XML output into RawFindings.
 *
 * Checkstyle XML structure:
 * <checkstyle>
 *   <file name="...">
 *     <error line="..." severity="..." message="..." source="..."/>
 *   </file>
 * </checkstyle>
 */
public class CheckstyleParser {

    public List<RawFinding> parse(String xmlOutput, AnalysisInput input) {
        List<RawFinding> findings = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlOutput.getBytes(StandardCharsets.UTF_8)));

            NodeList files = doc.getElementsByTagName("file");
            for (int i = 0; i < files.getLength(); i++) {
                Element file = (Element) files.item(i);
                String fileName = file.getAttribute("name");

                NodeList errors = file.getElementsByTagName("error");
                for (int j = 0; j < errors.getLength(); j++) {
                    Element error = (Element) errors.item(j);
                    int line = parseIntSafe(error.getAttribute("line"));
                    String severity = error.getAttribute("severity");
                    String message = error.getAttribute("message");
                    String source = error.getAttribute("source");

                    findings.add(new RawFinding(
                            FindingSource.STATIC,
                            FindingCategory.QUALITY,
                            mapSeverity(severity),
                            100,
                            extractRuleName(source),
                            message,
                            "Fix style issue: " + extractRuleName(source),
                            resolveFilePath(fileName, input),
                            line,
                            line
                    ));
                }
            }
        } catch (Exception e) {
            // Malformed XML: return empty
        }
        return findings;
    }

    private Severity mapSeverity(String severity) {
        if (severity == null) return Severity.LOW;
        return switch (severity.toLowerCase()) {
            case "error" -> Severity.MEDIUM;
            case "warning" -> Severity.LOW;
            case "info" -> Severity.INFO;
            default -> Severity.LOW;
        };
    }

    private String extractRuleName(String source) {
        if (source == null || source.isEmpty()) return "checkstyle-violation";
        int lastDot = source.lastIndexOf('.');
        return lastDot >= 0 ? source.substring(lastDot + 1) : source;
    }

    private String resolveFilePath(String absolutePath, AnalysisInput input) {
        for (var file : input.files()) {
            if (absolutePath.endsWith(file.filePath())) {
                return file.filePath();
            }
        }
        return absolutePath;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
