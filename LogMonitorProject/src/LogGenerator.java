import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/** Creates a reproducible synthetic application log (fixed random seed). */
public class LogGenerator {
    static final String[] COMPONENTS = {"AuthService", "PaymentService", "OrderService", "InventoryService", "DatabasePool", "ApiGateway"};
    static final String[] ERRORS = {
        "Database connection timeout after %d ms", "NullPointerException in OrderProcessor.process",
        "Payment gateway returned HTTP 502", "Failed to acquire lock on inventory item %d",
        "Authentication failed for user id %d", "OutOfMemoryError: Java heap space", "Disk quota exceeded on volume /data"};
    static final int[] ERROR_WEIGHTS = {30, 22, 18, 14, 9, 4, 3};   // out of 100
    static final String[] WARNS = {"Slow query detected: %d ms", "Retry attempt %d for request %d",
        "Cache miss ratio above threshold", "Connection pool at 90% capacity"};
    static final String[] INFOS = {"Request %d completed in %d ms", "User %d logged in", "Order %d created", "Scheduled job finished"};
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    static String line(Random rnd, LocalDateTime t) {
        int p = rnd.nextInt(100);
        String level, msg;
        if (p < 72)      { level = "INFO";  msg = fill(INFOS[rnd.nextInt(INFOS.length)], rnd); }
        else if (p < 87) { level = "WARN";  msg = fill(WARNS[rnd.nextInt(WARNS.length)], rnd); }
        else if (p < 97) { level = "ERROR"; msg = fill(ERRORS[pickWeighted(rnd)], rnd); }
        else             { level = "DEBUG"; msg = "Heartbeat ok, queue size " + rnd.nextInt(50); }
        return FMT.format(t) + " [" + level + "] [" + COMPONENTS[rnd.nextInt(COMPONENTS.length)] + "] " + msg;
    }
    static int pickWeighted(Random rnd) {
        int x = rnd.nextInt(100), acc = 0;
        for (int i = 0; i < ERROR_WEIGHTS.length; i++) { acc += ERROR_WEIGHTS[i]; if (x < acc) return i; }
        return 0;
    }
    static String fill(String t, Random rnd) {
        return t.replace("%d", "\u0000").chars().collect(StringBuilder::new,
            (sb, c) -> sb.append(c == 0 ? String.valueOf(1 + rnd.nextInt(9999)) : String.valueOf((char) c)), StringBuilder::append).toString();
    }

    static void generate(Path file, long lines) throws IOException {
        Random rnd = new Random(42);
        LocalDateTime t = LocalDateTime.of(2026, 11, 1, 0, 0, 0);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (long i = 0; i < lines; i++) {
                t = t.plusNanos((1 + rnd.nextInt(3)) * 1_000_000L);
                w.write(line(rnd, t)); w.write('\n');
            }
        }
    }
}
