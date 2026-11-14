/**
 * Parses one line of the form:
 * 2026-11-10 14:23:45.123 [ERROR] [PaymentService] Payment gateway returned HTTP 502
 */
public class LogParser {
    static void process(String line, AnalysisResult r) {
        r.totalLines++;
        int a = line.indexOf('[');
        int b = a < 0 ? -1 : line.indexOf(']', a);
        if (b < 0) { r.levelCounts.merge("UNKNOWN", 1, Integer::sum); return; }
        String level = line.substring(a + 1, b);
        r.levelCounts.merge(level, 1, Integer::sum);
        if (level.equals("ERROR") || level.equals("WARN")) {
            int c = line.indexOf(']', line.indexOf('[', b) + 1);   // end of [Component]
            String msg = normalise(c < 0 ? line.substring(b + 1) : line.substring(c + 1).trim());
            (level.equals("ERROR") ? r.errorCounts : r.warnCounts).merge(msg, 1, Integer::sum);
        }
    }

    /** Replaces each run of digits with '#', so "timeout after 4821 ms" and "... 977 ms" count as one message. */
    static String normalise(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inDigits = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') { if (!inDigits) sb.append('#'); inDigits = true; }
            else { sb.append(ch); inDigits = false; }
        }
        return sb.toString();
    }
}
