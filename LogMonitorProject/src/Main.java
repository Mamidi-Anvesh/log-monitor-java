import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    interface Job { AnalysisResult run() throws Exception; }

    static double timeMs(Job j, AnalysisResult[] out) throws Exception {
        long t0 = System.nanoTime();
        out[0] = j.run();
        return (System.nanoTime() - t0) / 1e6;
    }

    public static void main(String[] a) throws Exception {
        switch (a[0]) {
            case "generate" -> { LogGenerator.generate(Path.of(a[1]), Long.parseLong(a[2])); System.out.println("generated " + a[1]); }
            case "analyze" -> {
                Path f = Path.of(a[1]);
                AnalysisResult r = switch (a[2]) {
                    case "io" -> TraditionalAnalyzer.analyze(f);
                    case "files" -> NioPathAnalyzer.analyzeStreaming(f);
                    case "all" -> NioPathAnalyzer.analyzeAllLines(f);
                    case "fast" -> NioChannelAnalyzer.analyzeFast(f, 64 * 1024);
                    default -> NioChannelAnalyzer.analyze(f, 64 * 1024, true);
                };
                r.printSummary(7);
            }
            case "bench" -> bench(Path.of(a[1]), Arrays.stream(a[2].split(",")).mapToLong(Long::parseLong).toArray());
            case "buffers" -> buffers(Path.of(a[1]));
            case "monitor-demo" -> monitorDemo(Path.of(a[1]));
            default -> System.err.println("unknown command");
        }
    }

    static void bench(Path dir, long[] sizes) throws Exception {
        Files.createDirectories(dir);
        System.out.println("lines,sizeMB,io_ms,files_ms,channel_basic_ms,channel_fast_ms,identical");
        for (long n : sizes) {
            Path f = dir.resolve("app_" + n + ".log");
            LogGenerator.generate(f, n);
            double mb = Files.size(f) / 1048576.0;
            Job[] jobs = {
                () -> TraditionalAnalyzer.analyze(f), () -> NioPathAnalyzer.analyzeStreaming(f),
                () -> NioChannelAnalyzer.analyze(f, 64 * 1024, true), () -> NioChannelAnalyzer.analyzeFast(f, 64 * 1024) };
            int J = jobs.length;
            AnalysisResult[][] outs = new AnalysisResult[J][1];
            for (int j = 0; j < J; j++) timeMs(jobs[j], outs[j]);            // warm-up run
            double[] sum = new double[J]; int runs = 5;
            for (int k = 0; k < runs; k++) for (int j = 0; j < J; j++) sum[j] += timeMs(jobs[j], outs[j]);
            boolean same = true;
            for (int j = 1; j < J; j++) same &= outs[0][0].equals(outs[j][0]);
            System.out.printf(Locale.ROOT, "%d,%.1f,%.0f,%.0f,%.0f,%.0f,%b%n", n, mb, sum[0] / runs, sum[1] / runs, sum[2] / runs, sum[3] / runs, same);
        }
    }

    static void buffers(Path f) throws Exception {
        System.out.println("bufferKB,basic_direct_ms,basic_heap_ms,fast_heap_ms");
        for (int kb : new int[]{1, 4, 8, 64, 256}) {
            double[] s = new double[3]; int runs = 5;
            AnalysisResult[] o = new AnalysisResult[1];
            final int size = kb * 1024;
            Job[] jobs = { () -> NioChannelAnalyzer.analyze(f, size, true), () -> NioChannelAnalyzer.analyze(f, size, false),
                           () -> NioChannelAnalyzer.analyzeFast(f, size) };
            for (int d = 0; d < 3; d++) {
                timeMs(jobs[d], o);
                for (int k = 0; k < runs; k++) s[d] += timeMs(jobs[d], o);
            }
            System.out.printf(Locale.ROOT, "%d,%.0f,%.0f,%.0f%n", kb, s[0] / runs, s[1] / runs, s[2] / runs);
        }
    }

    static void monitorDemo(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path f = dir.resolve("live.log");
        Files.writeString(f, "2026-11-14 10:00:00.000 [INFO] [ApiGateway] Service started\n");
        LogMonitor mon = new LogMonitor(f, 3, 3000);
        Thread t = new Thread(() -> { try { mon.run(); } catch (Exception e) { e.printStackTrace(); } });
        t.start();
        Thread.sleep(500);
        Random rnd = new Random(7);
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 11, 14, 10, 0, 1);
        String[] msgs = {
            "[INFO] [OrderService] Order 1201 created", "[WARN] [DatabasePool] Connection pool at 90% capacity",
            "[ERROR] [PaymentService] Payment gateway returned HTTP 502", "[ERROR] [DatabasePool] Database connection timeout after 5012 ms",
            "[ERROR] [PaymentService] Payment gateway returned HTTP 502", "[INFO] [AuthService] User 77 logged in",
            "[ERROR] [PaymentService] Payment gateway returned HTTP 502", "[INFO] [ApiGateway] Request 9 completed in 31 ms" };
        try (BufferedWriter w = Files.newBufferedWriter(f, java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            for (String m : msgs) {
                now = now.plusNanos(300_000_000L);
                w.write(LogGenerator.FMT.format(now) + " " + m + "\n"); w.flush();
                Thread.sleep(400);
            }
        }
        Thread.sleep(600);
        mon.running = false; t.join();
        System.out.println("[monitor] stopped. Live summary:");
        mon.result().printSummary(3);
    }
}
