package org.example.Creations;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * Animated loading panel for the console: spinner, gradient progress bar with a moving shine,
 * done / total counter and elapsed time.
 * <p>
 * Usage:
 * <pre>
 * Loading loading = new Loading("Waiting for work orders to reach SCHEDULE", 12);
 * loading.start();
 * loading.set_done(7);                       // call whenever the count changes
 * loading.finish(true, "All 12 work orders are SCHEDULE");
 * </pre>
 * Colors and Unicode are used when the console supports them (Windows console, Windows Terminal,
 * IntelliJ, Linux/macOS terminals); otherwise a plain one-line ASCII version is shown.
 */
public class Loading
{
    private static final int WIDTH = 64;        // inner width of the panel, in characters
    private static final int BAR_WIDTH = 34;
    private static final int FRAME_MS = 80;
    private static final String TITLE = " ◆ PERFORMANCE TEST DATA ";
    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final String[] PLAIN_SPINNER = {"|", "/", "-", "\\"};

    // ANSI escape codes
    private static final String ESC = "\u001b[";
    private static final String RESET = ESC + "0m";
    private static final String BOLD = ESC + "1m";
    private static final String DIM = ESC + "2m";

    private static Boolean fancy_console = null;   // detected once, on first use
    private static MethodHandle set_output_cp;      // to give the console its code page back at exit

    private final String message;
    private final int total;
    private final PrintStream out;
    private final boolean fancy;

    private volatile int done = 0;
    private long start_time;
    private int frame = 0;
    private boolean drawn_once = false;
    private int plain_length = 0;   // length of the last plain line, to clear it on the next draw
    private boolean finished = false;
    private Thread animator;

    public Loading(String message, int total) {
        this.message = message;
        this.total = total;
        this.fancy = is_fancy_console();
        // Our own UTF-8 stream, so the spinner and bar characters are not turned into '?'
        this.out = fancy ? new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8) : System.out;
    }

    /** Starts the animation in the background. */
    public void start() {
        start_time = System.currentTimeMillis();
        if (fancy) {
            out.print(ESC + "?25l");   // hide the cursor while animating
        }
        animator = new Thread(() -> {
            while (true) {
                synchronized (this) {
                    if (finished) {
                        return;
                    }
                    draw(null, false);
                    frame++;
                }
                try {
                    Thread.sleep(FRAME_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        animator.setDaemon(true);
        animator.start();
    }

    /** Sets how many of the total are complete; the bar moves to match. */
    public void set_done(int done) { this.done = Math.max(0, Math.min(done, total)); }

    /** Stops the animation and draws the final state: a green check on success, a red cross otherwise. */
    public void finish(boolean success, String result_message) {
        synchronized (this) {
            if (finished) {
                return;
            }
            finished = true;
            draw(result_message, success);
            if (fancy) {
                out.print(ESC + "?25h");   // show the cursor again
            }
            out.println();
            out.flush();
        }
        if (animator != null) {
            animator.interrupt();
        }
    }

    /** Draws the current frame, or the final frame when result_message is not null. */
    private void draw(String result_message, boolean success) {
        if (fancy) {
            draw_panel(result_message, success);
        } else {
            draw_plain(result_message, success);
        }
        out.flush();
    }

    // ---------------------------------------------------------------- fancy panel

    private void draw_panel(String result_message, boolean success) {
        boolean final_frame = result_message != null;
        int[] border = !final_frame ? new int[]{90, 110, 160} : success ? new int[]{60, 200, 120} : new int[]{230, 70, 80};
        String b = rgb(border);

        StringBuilder sb = new StringBuilder();
        if (drawn_once) {
            sb.append(ESC).append(7).append('A');   // go back up to the top of the panel
        }
        drawn_once = true;

        // Top border with title
        int dashes = WIDTH - 1 - TITLE.length();
        sb.append(ESC).append("2K\r  ").append(b).append("╭─").append(RESET)
          .append(BOLD).append(gradient_text(TITLE)).append(RESET)
          .append(b).append("─".repeat(dashes)).append("╮").append(RESET).append('\n');

        sb.append(row(b, "", 0));

        // Spinner / result icon and message
        String icon;
        String text;
        if (!final_frame) {
            icon = rgb(spinner_color()) + BOLD + SPINNER[frame % SPINNER.length] + RESET;
            text = message + dots();
        } else {
            icon = rgb(border) + BOLD + (success ? "✔" : "✖") + RESET;
            text = result_message;
        }
        text = fit(text, WIDTH - 6);
        sb.append(row(b, "  " + icon + "  " + BOLD + text + RESET, 5 + text.length()));

        // Progress bar
        double ratio = total == 0 ? 1.0 : (double) done / total;
        int filled = (int) Math.round(ratio * BAR_WIDTH);
        String counter = String.format("%d / %d", done, total);
        String percent = String.format("%3d%%", Math.round(ratio * 100));
        String bar = bar(filled, final_frame, success);
        String bar_line = "  " + bar + "  " + BOLD + counter + RESET + "  " + DIM + percent + RESET;
        sb.append(row(b, bar_line, 2 + BAR_WIDTH + 2 + counter.length() + 2 + percent.length()));

        // Timing
        long elapsed = (System.currentTimeMillis() - start_time) / 1000;
        String info = "elapsed " + clock(elapsed);
        sb.append(row(b, "  " + DIM + info + RESET, 2 + info.length()));

        sb.append(row(b, "", 0));

        sb.append(ESC).append("2K\r  ").append(b).append("╰").append("─".repeat(WIDTH)).append("╯").append(RESET);
        sb.append('\n');
        out.print(sb);
    }

    /** One line inside the panel: borders on both sides, content padded to the panel width. */
    private static String row(String border_color, String content, int visible_length) {
        return ESC + "2K\r  " + border_color + "│" + RESET + content
             + " ".repeat(Math.max(0, WIDTH - visible_length))
             + border_color + "│" + RESET + "\n";
    }

    /** The filled part fades from cyan to violet, with a bright shine sliding across it. */
    private String bar(int filled, boolean final_frame, boolean success) {
        StringBuilder sb = new StringBuilder();
        int shine = frame % (BAR_WIDTH + 12) - 6;
        for (int i = 0; i < BAR_WIDTH; i++) {
            if (i < filled) {
                int[] color;
                if (final_frame) {
                    color = success ? mix(new int[]{40, 180, 110}, new int[]{120, 230, 160}, (double) i / BAR_WIDTH)
                                    : new int[]{230, 70, 80};
                } else {
                    color = mix(new int[]{0, 210, 255}, new int[]{190, 90, 255}, (double) i / BAR_WIDTH);
                    int distance = Math.abs(i - shine);
                    if (distance < 4) {
                        color = mix(color, new int[]{255, 255, 255}, 0.55 * (1 - distance / 4.0));
                    }
                }
                sb.append(rgb(color)).append('█');
            } else {
                sb.append(rgb(new int[]{55, 60, 80})).append('░');
            }
        }
        return sb.append(RESET).toString();
    }

    private static String gradient_text(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(rgb(mix(new int[]{0, 210, 255}, new int[]{230, 110, 255}, (double) i / text.length())))
              .append(text.charAt(i));
        }
        return sb.toString();
    }

    /** The spinner slowly cycles through the same cyan-to-violet colors as the bar. */
    private int[] spinner_color() {
        double t = (Math.sin(frame / 6.0) + 1) / 2;
        return mix(new int[]{0, 210, 255}, new int[]{190, 90, 255}, t);
    }

    /** Animated "...", always 3 characters wide so the line does not jump. */
    private String dots() {
        int count = (frame / 5) % 4;
        return ".".repeat(count) + " ".repeat(3 - count);
    }

    // ---------------------------------------------------------------- plain fallback

    private void draw_plain(String result_message, boolean success) {
        double ratio = total == 0 ? 1.0 : (double) done / total;
        int filled = (int) Math.round(ratio * 20);
        String bar = "[" + "#".repeat(filled) + "-".repeat(20 - filled) + "]";
        long elapsed = (System.currentTimeMillis() - start_time) / 1000;
        String icon = result_message == null ? PLAIN_SPINNER[frame % PLAIN_SPINNER.length] : success ? "OK" : "X";
        String text = result_message == null ? message : result_message;
        String line = String.format("[%s] %s %s %d/%d %3d%% %s", icon, text, bar, done, total, Math.round(ratio * 100), clock(elapsed));
        // Pad with spaces so a shorter line fully covers the previous one
        out.print("\r" + line + " ".repeat(Math.max(0, plain_length - line.length())));
        plain_length = line.length();
    }

    // ---------------------------------------------------------------- helpers

    private static String rgb(int[] c) { return ESC + "38;2;" + c[0] + ";" + c[1] + ";" + c[2] + "m"; }

    private static int[] mix(int[] a, int[] b, double t) {
        return new int[]{
            (int) Math.round(a[0] + (b[0] - a[0]) * t),
            (int) Math.round(a[1] + (b[1] - a[1]) * t),
            (int) Math.round(a[2] + (b[2] - a[2]) * t)};
    }

    private static String clock(long seconds) { return String.format("%02d:%02d", seconds / 60, seconds % 60); }

    private static String fit(String text, int max) { return text.length() <= max ? text : text.substring(0, max - 1) + "…"; }

    // ---------------------------------------------------------------- console detection

    /**
     * True when the console can show colors and Unicode.
     * On Windows this switches the console to ANSI colors and UTF-8 (the code page is restored at exit).
     */
    private static synchronized boolean is_fancy_console() {
        if (fancy_console == null) {
            if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
                fancy_console = true;
            } else {
                fancy_console = enable_windows_console() || started_from_intellij();
            }
        }
        return fancy_console;
    }

    private static boolean enable_windows_console() {
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
            MethodHandle get_std_handle = linker.downcallHandle(kernel32.find("GetStdHandle").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            MethodHandle get_console_mode = linker.downcallHandle(kernel32.find("GetConsoleMode").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            MethodHandle set_console_mode = linker.downcallHandle(kernel32.find("SetConsoleMode").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            MethodHandle get_output_cp = linker.downcallHandle(kernel32.find("GetConsoleOutputCP").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT));
            set_output_cp = linker.downcallHandle(kernel32.find("SetConsoleOutputCP").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            MemorySegment handle = (MemorySegment) get_std_handle.invokeExact(-11);   // STD_OUTPUT_HANDLE
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment mode = arena.allocate(ValueLayout.JAVA_INT);
                if ((int) get_console_mode.invokeExact(handle, mode) == 0) {
                    return false;   // output is not a real console (redirected or an IDE)
                }
                int enable_virtual_terminal = 0x0004;
                if ((int) set_console_mode.invokeExact(handle, mode.get(ValueLayout.JAVA_INT, 0) | enable_virtual_terminal) == 0) {
                    return false;
                }
            }

            int original_cp = (int) get_output_cp.invokeExact();
            int ignored = (int) set_output_cp.invokeExact(65001);   // UTF-8
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.print(ESC + "?25h");
                System.out.flush();
                try {
                    int restored = (int) set_output_cp.invokeExact(original_cp);
                } catch (Throwable e) {
                    // nothing more we can do while shutting down
                }
            }));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** IntelliJ's Run window is not a real console, but it shows colors and UTF-8. */
    private static boolean started_from_intellij() {
        String command = ProcessHandle.current().info().commandLine().orElse("");
        return command.contains("idea_rt") || command.contains("JetBrains");
    }

    /** Preview of the loading screen with fake progress: run this class directly. */
    public static void main(String[] args) throws InterruptedException {
        int total = 12;
        Loading loading = new Loading("Waiting for work orders to reach SCHEDULE", total);
        loading.start();
        for (int i = 0; i <= total; i++) {
            loading.set_done(i);
            Thread.sleep(600);
        }
        loading.finish(true, "All " + total + " work orders are SCHEDULE");
    }
}
