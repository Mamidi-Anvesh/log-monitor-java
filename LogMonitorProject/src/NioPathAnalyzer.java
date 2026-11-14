import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/** NIO.2: Path and Files. Streaming (Files.lines) and the non-streaming Files.readAllLines. */
public class NioPathAnalyzer {
    static AnalysisResult analyzeStreaming(Path file) throws IOException {
        AnalysisResult r = new AnalysisResult();
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.forEach(l -> LogParser.process(l, r));
        }
        return r;
    }
    /** Loads the whole file into memory first: simple, but memory grows with file size. */
    static AnalysisResult analyzeAllLines(Path file) throws IOException {
        AnalysisResult r = new AnalysisResult();
        List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String l : all) LogParser.process(l, r);
        return r;
    }
}
