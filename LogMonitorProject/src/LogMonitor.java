import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

/** Real-time monitor: a WatchService reports file modifications; only the newly appended bytes are read through a FileChannel. */
public class LogMonitor {
    private final Path file;
    private final int alertThreshold;
    private final long windowMillis;
    private final AnalysisResult live = new AnalysisResult();
    private final ArrayDeque<Long> recentErrors = new ArrayDeque<>();
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private long position;
    private long lastAlert = 0;
    volatile boolean running = true;

    LogMonitor(Path file, int alertThreshold, long windowMillis) {
        this.file = file; this.alertThreshold = alertThreshold; this.windowMillis = windowMillis;
    }

    AnalysisResult result() { return live; }

    void run() throws IOException, InterruptedException {
        position = Files.size(file);                       // like "tail -f": start at the current end
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            file.toAbsolutePath().getParent().register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
            System.out.println("[monitor] watching " + file.getFileName() + " from byte " + position);
            while (running) {
                WatchKey key = ws.poll(200, TimeUnit.MILLISECONDS);
                if (key == null) continue;
                boolean changed = false;
                for (WatchEvent<?> ev : key.pollEvents())
                    if (ev.context() instanceof Path p && p.equals(file.getFileName())) changed = true;
                if (changed) readAppended();
                if (!key.reset()) break;
            }
            readAppended();                               // final drain
        }
    }

    private void readAppended() throws IOException {
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            long size = ch.size();
            if (size < position) { position = 0; pending.reset(); }   // file was rotated or truncated
            ch.position(position);
            ByteBuffer buf = ByteBuffer.allocate(8192);
            while (ch.read(buf) > 0) {
                buf.flip();
                while (buf.hasRemaining()) {
                    byte b = buf.get();
                    if (b == '\n') { handle(pending.toString(StandardCharsets.UTF_8)); pending.reset(); }
                    else pending.write(b);
                }
                buf.clear();
            }
            position = ch.position();
        }
    }

    private void handle(String line) {
        LogParser.process(line, live);
        if (line.contains("[ERROR]")) {
            long now = System.currentTimeMillis();
            recentErrors.addLast(now);
            while (!recentErrors.isEmpty() && now - recentErrors.peekFirst() > windowMillis) recentErrors.pollFirst();
            System.out.println("[monitor] ERROR   " + line.substring(24));
            if (recentErrors.size() >= alertThreshold && now - lastAlert > windowMillis) {
                lastAlert = now;
                System.out.println("[monitor] *** ALERT: " + recentErrors.size() + " errors within " + windowMillis + " ms ***");
            }
        } else if (line.contains("[WARN]")) {
            System.out.println("[monitor] WARN    " + line.substring(24));
        }
    }
}
