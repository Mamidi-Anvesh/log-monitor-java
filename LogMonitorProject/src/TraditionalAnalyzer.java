import java.io.*;
import java.nio.file.Path;

/** Classic java.io: FileReader wrapped in a BufferedReader. */
public class TraditionalAnalyzer {
    static AnalysisResult analyze(Path file) throws IOException {
        AnalysisResult r = new AnalysisResult();
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) LogParser.process(line, r);
        }
        return r;
    }
}
