import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

/** NIO Channels and Buffers: reads the file in fixed-size blocks and splits lines manually. */
public class NioChannelAnalyzer {
    static AnalysisResult analyze(Path file, int bufferSize, boolean direct) throws IOException {
        AnalysisResult r = new AnalysisResult();
        ByteBuffer buf = direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
        byte[] line = new byte[512];
        int len = 0;
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            while (ch.read(buf) != -1) {
                buf.flip();
                while (buf.hasRemaining()) {
                    byte b = buf.get();
                    if (b == '\n') {
                        LogParser.process(new String(line, 0, len, StandardCharsets.ISO_8859_1), r);
                        len = 0;
                    } else {
                        if (len == line.length) line = Arrays.copyOf(line, len * 2);
                        line[len++] = b;
                    }
                }
                buf.clear();
            }
            if (len > 0) LogParser.process(new String(line, 0, len, StandardCharsets.ISO_8859_1), r);  // last line without newline
        }
        return r;
    }

    /** Optimised version: heap buffer, scans the backing array in bulk and decodes each line straight from it. */
    static AnalysisResult analyzeFast(Path file, int bufferSize) throws IOException {
        AnalysisResult r = new AnalysisResult();
        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
        byte[] arr = buf.array();
        byte[] carry = new byte[512];
        int carryLen = 0;
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            while (ch.read(buf) != -1) {
                int end = buf.position(), start = 0;
                for (int i = 0; i < end; i++) {
                    if (arr[i] != '\n') continue;
                    String s;
                    if (carryLen > 0) {
                        int n = i - start;
                        if (carryLen + n > carry.length) carry = Arrays.copyOf(carry, Math.max(carry.length * 2, carryLen + n));
                        System.arraycopy(arr, start, carry, carryLen, n);
                        s = new String(carry, 0, carryLen + n, StandardCharsets.ISO_8859_1);
                        carryLen = 0;
                    } else {
                        s = new String(arr, start, i - start, StandardCharsets.ISO_8859_1);
                    }
                    LogParser.process(s, r);
                    start = i + 1;
                }
                int rem = end - start;                       // incomplete last line: keep for the next read
                if (rem > 0) {
                    if (carryLen + rem > carry.length) carry = Arrays.copyOf(carry, Math.max(carry.length * 2, carryLen + rem));
                    System.arraycopy(arr, start, carry, carryLen, rem);
                    carryLen += rem;
                }
                buf.clear();
            }
            if (carryLen > 0) LogParser.process(new String(carry, 0, carryLen, StandardCharsets.ISO_8859_1), r);
        }
        return r;
    }
}
