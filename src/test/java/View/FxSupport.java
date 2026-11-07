package View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Window;

/**
 * Test helper that runs real JavaFX screens inside JUnit.
 *
 * It starts the JavaFX toolkit once, runs code on the
 * FX Application Thread and searches the real scene graph.
 * No mouse or keyboard robot is used: buttons are fired and
 * pieces are clicked through their own event handlers.
 */
final class FxSupport {

    private static boolean started;

    private FxSupport() {
    }

    static synchronized void init() throws Exception {
        if (started) {
            return;
        }
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        if (!ready.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start");
        }
        Platform.setImplicitExit(false);
        Motion.setSpeed(30.0);
        started = true;
    }

    /** Runs code on the FX thread and returns its result. */
    static <T> T fx(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    static void fx(Runnable action) throws Exception {
        fx(() -> {
            action.run();
            return null;
        });
    }

    /** Polls a condition (evaluated on the FX thread) until true. */
    static void waitUntil(String what, BooleanSupplier condition,
                          long timeoutMillis) throws Exception {
        long end = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < end) {
            if (fx(() -> condition.getAsBoolean())) {
                return;
            }
            Thread.sleep(15);
        }
        throw new AssertionError("Timed out waiting for: " + what);
    }

    /** Collects every node below root that matches the filter. */
    static List<Node> find(Node root, Predicate<Node> filter) {
        List<Node> result = new ArrayList<>();
        collect(root, filter, result);
        return result;
    }

    private static void collect(Node n, Predicate<Node> f, List<Node> out) {
        if (f.test(n)) {
            out.add(n);
        }
        if (n instanceof Parent p) {
            for (Node child : new ArrayList<>(p.getChildrenUnmodifiable())) {
                collect(child, f, out);
            }
        }
    }

    /** Applies CSS and layout so skins and scroll content exist. */
    static void settle(javafx.scene.Scene scene) throws Exception {
        fx(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
        });
    }

    /** True when the node and all of its parents are visible and managed. */
    static boolean shown(Node n) {
        for (Node cur = n; cur != null; cur = cur.getParent()) {
            if (cur != n && (cur.getClass().isAnonymousClass()
                    || cur instanceof javafx.scene.control.ScrollPane)) {
                return true;   // scroll-pane skin internals are not game UI
            }
            if (!cur.isVisible() || !cur.isManaged()) {
                return false;
            }
        }
        return true;
    }

    static Button button(Node root, String text) {
        return (Button) find(root, n -> n instanceof Button b
                && text.equals(b.getText())).stream().findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No button labelled '" + text + "'"));
    }

    /** Fires the button whose text matches, like a user click. */
    static void click(Node root, String text) throws Exception {
        fx(() -> {
            Button b = button(root, text);
            if (b.isDisable()) {
                throw new AssertionError("Button '" + text + "' is disabled");
            }
            b.fire();
        });
    }

    /** Hides every window, so tests never leak open stages. */
    static void closeAllWindows() throws Exception {
        fx(() -> new ArrayList<>(Window.getWindows()).forEach(Window::hide));
    }

    /** True if some window other than the given one shows this button. */
    static boolean dialogHasButton(Window main, String text) {
        for (Window w : Window.getWindows()) {
            if (w != main && w.isShowing() && w.getScene() != null
                    && !find(w.getScene().getRoot(), n -> n instanceof Button b
                            && text.equals(b.getText())).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Closes any modal dialog that is currently showing. */
    static void closeDialogs(String preferredButton) throws Exception {
        fx(() -> {
            for (Window w : new ArrayList<>(Window.getWindows())) {
                if (w.isShowing() && w.getScene() != null
                        && w.getScene().getRoot() != null) {
                    List<Node> buttons = find(w.getScene().getRoot(),
                            n -> n instanceof Button b
                                    && preferredButton.equals(b.getText()));
                    if (!buttons.isEmpty()) {
                        ((Button) buttons.get(0)).fire();
                    }
                }
            }
        });
    }
}
