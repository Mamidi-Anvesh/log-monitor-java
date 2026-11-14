import java.util.*;

/** Holds the counts produced by an analyzer. HashMap stores the summaries, TreeMap sorts them. */
public class AnalysisResult {
    long totalLines;
    final Map<String, Integer> levelCounts = new HashMap<>();
    final Map<String, Integer> errorCounts = new HashMap<>();
    final Map<String, Integer> warnCounts  = new HashMap<>();

    /** Frequency -> messages with that frequency, highest frequency first (TreeMap with reverse order). */
    static TreeMap<Integer, List<String>> sortByFrequency(Map<String, Integer> counts) {
        TreeMap<Integer, List<String>> sorted = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<String, Integer> e : counts.entrySet())
            sorted.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        for (List<String> l : sorted.values()) Collections.sort(l);
        return sorted;
    }

    void printSummary(int topN) {
        System.out.println("Total lines : " + totalLines);
        System.out.println("Level counts: " + new TreeMap<>(levelCounts));
        System.out.println("Top " + topN + " errors (TreeMap order):");
        int shown = 0;
        for (Map.Entry<Integer, List<String>> e : sortByFrequency(errorCounts).entrySet())
            for (String m : e.getValue()) {
                if (shown++ >= topN) return;
                System.out.printf("  %7d  %s%n", e.getKey(), m);
            }
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof AnalysisResult r)) return false;
        return totalLines == r.totalLines && levelCounts.equals(r.levelCounts)
            && errorCounts.equals(r.errorCounts) && warnCounts.equals(r.warnCounts);
    }
    @Override public int hashCode() { return Objects.hash(totalLines, levelCounts, errorCounts, warnCounts); }
}
