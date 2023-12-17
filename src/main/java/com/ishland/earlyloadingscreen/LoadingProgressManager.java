package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LoadingProgressManager {

    private static final CopyOnWriteArrayList<Progress> activeProgress = new CopyOnWriteArrayList<>();

    static {
        ProgressHolder.class.getName(); // load class
        Progress.class.getName(); // load class
    }

    static CopyOnWriteArrayList<Progress> getActiveProgress() {
        return activeProgress;
    }

    public static ProgressHolder tryCreateProgressHolder() {
        return new ProgressHolder();
    }

    public static void showMessageAsProgress(String message) {
        showMessageAsProgress(message, 10000L);
    }

    public static void showMessageAsProgress(String message, long timeMillis) {
        final ProgressHolder holder = tryCreateProgressHolder();
        if (holder != null) {
            final ScheduledFuture<?> future = LoadingScreenManager.SCHEDULER.schedule(holder::close, timeMillis, TimeUnit.MILLISECONDS);
            holder.update(() -> String.format("(%ds) %s", future.getDelay(TimeUnit.SECONDS), message));
            holder.updateProgress(() -> 1f - (float) future.getDelay(TimeUnit.MILLISECONDS) / (float) timeMillis);
        }
    }

    static class Progress {
        private volatile Supplier<String> supplier;
        private String text = "";
        private volatile Supplier<Float> progressSupplier;

        public void update(Supplier<String> text) {
            this.supplier = text;
        }

        public void updateProgress(Supplier<Float> progressSupplier) {
            this.progressSupplier = progressSupplier;
        }

        public String get() {
            final Supplier<String> supplier = this.supplier;
            if (supplier == null) return "";
            return supplier.get();
        }

        public float getProgress() {
            final Supplier<Float> floatSupplier = this.progressSupplier;
            return floatSupplier != null ? floatSupplier.get() : 0.0f;
        }

        private String get0() {
            try {
                return supplier.get();
            } catch (Throwable t) {
                return "Error: " + t.getMessage();
            }
        }

    }

    public static class ProgressHolder implements AppLoaderAccessSupport.ProgressHolderAccessor {

        private final Progress impl;

        public ProgressHolder() {
            Progress progress = this.impl = new Progress();
            LoadingScreenManager.CLEANER.register(this, () -> {
                activeProgress.remove(progress);
            });
            activeProgress.add(impl);
        }

        public void update(Supplier<String> text) {
            impl.update(text);
        }

        public void updateProgress(Supplier<Float> progress) {
            impl.updateProgress(progress);
        }

        @Override
        public void close() {
            activeProgress.remove(impl);
        }
    }

}
