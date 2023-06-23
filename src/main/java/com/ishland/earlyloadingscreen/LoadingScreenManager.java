package com.ishland.earlyloadingscreen;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.ishland.earlyloadingscreen.render.GLText;
import com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static com.ishland.earlyloadingscreen.SharedConstants.LOGGER;
import static com.ishland.earlyloadingscreen.render.GLText.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL32.*;

public class LoadingScreenManager {

    public static final Cleaner CLEANER = Cleaner.create();

    private static long handle;
    public static final WindowEventLoop windowEventLoop;
    private static final Object windowEventLoopSync = new Object();

    static {
        LOGGER.info("Creating early window...");
        initGLFW();
        handle = initWindow();
        windowEventLoop = new WindowEventLoop(handle);
        windowEventLoop.start();
        AppLoaderAccessSupport.setAccess(LoadingScreenManager::tryCreateProgressHolder);
    }

    public static void init() {
        // intentionally empty
    }

    public static long takeContext() {
        synchronized (windowEventLoopSync) {
            if (handle == 0L) {
                return 0L;
            }
            LOGGER.info("Handing early window to Minecraft...");
            windowEventLoop.running.set(false);
            while (windowEventLoop.isAlive()) {
                LockSupport.parkNanos("Waiting for window event loop to exit", 100_000L);
            }
            final long handle1 = handle;
            handle = 0L;
//            if (SharedConstants.REUSE_WINDOW) {
//                return handle1;
//            } else {
//                LOGGER.info("Destroying early window...");
//                windowEventLoop.renderLoop = null;
//                glfwDestroyWindow(handle1);
//                return -1;
//            }
            if (!Config.REUSE_EARLY_WINDOW) {
                windowEventLoop.renderLoop = null;
            }
            return handle1;
        }
    }

    public static void reInitLoop() {
        synchronized (windowEventLoopSync) {
            LOGGER.info("Reinitializing screen rendering...");
            windowEventLoop.renderLoop = new RenderLoop();
        }
    }

    private static void initGLFW() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
            int i = GLFW.glfwGetError(pointerBuffer);
            if (i != 0) {
                long l = pointerBuffer.get();
                String string1 = l == 0L ? "" : MemoryUtil.memUTF8(l);
                throw new IllegalStateException(String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", i, string1));
            }
        }
        List<String> list = Lists.newArrayList();
        GLFWErrorCallback gLFWErrorCallback = GLFW.glfwSetErrorCallback(
                (code, pointer) -> list.add(String.format(Locale.ROOT, "GLFW error during init: [0x%X]%s", code, pointer))
        );
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join(list));
        } else {
            for(String string : list) {
                LOGGER.error("GLFW error collected during initialization: {}", string);
            }

            _glfwSetErrorCallback(gLFWErrorCallback);
        }
    }

    private static void _glfwSetErrorCallback(GLFWErrorCallback gLFWErrorCallback) {
        GLFWErrorCallback old = GLFW.glfwSetErrorCallback(gLFWErrorCallback);
        if (old != null) {
            old.free();
        }
    }

    private static void throwGlError(int error, long description) {
        String string = "GLFW error " + error + ": " + MemoryUtil.memUTF8(description);
        TinyFileDialogs.tinyfd_messageBox(
                "Minecraft", string + ".\n\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).", "ok", "error", false
        );
        throw new RuntimeException(string);
    }

    private static long initWindow() {
        GLFW.glfwSetErrorCallback(LoadingScreenManager::throwGlError);
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, 1);
        final long handle = GLFW.glfwCreateWindow(854, 480, "Minecraft - initializing mods...", 0L, 0L);
        // Center window
        final long monitor = glfwGetPrimaryMonitor();
        if (monitor != 0L) {
            final GLFWVidMode vidmode = glfwGetVideoMode(monitor);
            if (vidmode != null) {
                glfwSetWindowPos(
                        handle,
                        (vidmode.width() - 854) / 2,
                        (vidmode.height() - 480) / 2
                );
            }
        }

        return handle;
    }

    public static RenderLoop.ProgressHolder tryCreateProgressHolder() {
        synchronized (windowEventLoopSync) {
            final LoadingScreenManager.RenderLoop renderLoop = LoadingScreenManager.windowEventLoop.renderLoop;
            return renderLoop != null ? renderLoop.new ProgressHolder() : null;
        }
    }

    public static class RenderLoop {

        public GLText glt = new GLText();
        public final GLText.GLTtext memoryUsage = gltCreateText();
        public final GLText.GLTtext fpsText = gltCreateText();
        private final GLText.GLTtext progressText = gltCreateText();

        private final Object progressSync = new Object();
        private final ReferenceSet<Progress> activeProgress = ReferenceSets.synchronize(new ReferenceLinkedOpenHashSet<>(), progressSync);

        public void render() {
            int[] height = new int[1];
            int[] width = new int[1];
            glfwGetFramebufferSize(glfwGetCurrentContext(), width, height);
            glViewport(0, 0, width[0], height[0]);
            glt.gltViewport(width[0], height[0]);

            try (final Closeable ignored = glt.gltBeginDraw()) {
                glt.gltColor(1.0f, 1.0f, 1.0f, 1.0f);

                gltSetText(this.memoryUsage, "Memory: %d/%d MiB".formatted(
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L,
                        Runtime.getRuntime().maxMemory() / 1024L / 1024L
                ));
                glt.gltDrawText2DAligned(
                        this.memoryUsage,
                        width[0],
                        0,
                        1.0f,
                        GLT_RIGHT, GLT_TOP
                );
                glt.gltDrawText2DAligned(
                        this.fpsText,
                        0,
                        0,
                        1.0f,
                        GLT_LEFT, GLT_TOP
                );

                StringBuilder sb = new StringBuilder();
                synchronized (progressSync) {
                    for (Progress progress : activeProgress) {
                        if (progress.text.isBlank()) continue;
                        sb.append(progress.text).append('\n');
                    }
                }
                gltSetText(this.progressText, sb.toString().trim());
                glt.gltDrawText2DAligned(
                        this.progressText,
                        0,
                        height[0],
                        1.0f,
                        GLT_LEFT, GLT_BOTTOM
                );

            } catch (IOException e) {
                throw new RuntimeException(e); // shouldn't happen
            }
        }

        private void terminate() {
            glt.gltTerminate();
        }

        private static class Progress {
            public volatile String text = "";

            public void update(String text) {
                this.text = text;
            }

        }

        public class ProgressHolder implements AppLoaderAccessSupport.ProgressHolderAccessor {

            private final Progress impl;

            public ProgressHolder() {
                final ReferenceSet<Progress> activeProgress1 = activeProgress;
                Progress progress = this.impl = new Progress();
                CLEANER.register(this, () -> activeProgress1.remove(progress));
                activeProgress.add(impl);
            }

            public void update(String text) {
                impl.update(text);
            }

            @Override
            public void close() {
                activeProgress.remove(impl);
            }
        }

    }

    public static class WindowEventLoop extends Thread implements Executor {

        private final long handle;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        public volatile RenderLoop renderLoop = null;

        private WindowEventLoop(long handle) {
            this.handle = handle;
        }

        @Override
        public void run() {
            try {
                GLFW.glfwMakeContextCurrent(this.handle);
                GL.createCapabilities();
                glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                GLFW.glfwSwapInterval(1);

                RenderLoop renderLoop = this.renderLoop = new RenderLoop();

                long lastFpsTime = System.nanoTime();
                int fpsCounter = 0;
                int fps = 0;

                long lastFrameTime = System.nanoTime();

                final GLText glt = renderLoop.glt;
                while (running.get()) {
                    {
                        Runnable runnable;
                        while ((runnable = queue.poll()) != null) {
                            try {
                                runnable.run();
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                    }
                    if (GLFW.glfwWindowShouldClose(this.handle)) {
                        GLFW.glfwSetWindowShouldClose(this.handle, false);
                    }
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                    renderLoop.render();

                    GLFW.glfwPollEvents();
                    GLFW.glfwSwapBuffers(this.handle);
                    fpsCounter ++;
                    final long currentTime = System.nanoTime();
                    if (currentTime - lastFpsTime >= 1_000_000_000L) {
                        fps = (int) (fpsCounter * 1000_000_000L / (currentTime - lastFpsTime));
                        fpsCounter = 0;
                        lastFpsTime = currentTime;
                        gltSetText(renderLoop.fpsText, "%d fps".formatted(fps));
                    }
                    while ((System.nanoTime() - lastFrameTime) < 1_000_000_000L / 60L) {
                        LockSupport.parkNanos(1_000_000L);
                    }
                    lastFrameTime = System.nanoTime();
                }
            } catch (Throwable t) {
                LOGGER.error("Failed to render early window, exiting", t);
                this.renderLoop = null;
            } finally {
                Callbacks.glfwFreeCallbacks(this.handle);
                GLFW.glfwMakeContextCurrent(0L);
            }
        }

        @Override
        public void execute(@NotNull Runnable command) {
            queue.add(command);
        }

        public void setWindowTitle(CharSequence title) {
            GLFW.glfwSetWindowTitle(this.handle, title);
        }
    }

}
