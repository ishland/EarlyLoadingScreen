package com.ishland.earlyloadingscreen;

import com.ishland.earlyloadingscreen.platform_cl.AppLoaderAccessSupport;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LoadingProgressManager {

    private static final Set<Progress> activeProgress = new LinkedHashSet<>();

    static {
        ProgressHolder.class.getName(); // load class
        Progress.class.getName(); // load class
    }

    static Set<Progress> getActiveProgress() {
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
        }
    }

    static class Progress {
        private volatile Supplier<String> supplier;
        private int lastSupplierHash = 0;
        private String text = "";

        public void update(Supplier<String> text) {
            this.supplier = text;
        }

        public String get() {
            final Supplier<String> supplier = this.supplier;
            if (supplier == null) return "";
//            final int hash = System.identityHashCode(supplier);
//            if (hash != lastSupplierHash) {
//                lastSupplierHash = hash;
//                text = get0();
//            }
//            return text;
            return supplier.get();
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
            final Set<Progress> activeProgress1 = activeProgress;
            Progress progress = this.impl = new Progress();
            LoadingScreenManager.CLEANER.register(this, () -> {
                synchronized (activeProgress1) {
                    activeProgress1.remove(progress);
                }
            });
            synchronized (activeProgress1) {
                activeProgress1.add(impl);
            }
        }

        public void update(Supplier<String> text) {
            impl.update(text);
        }

        @Override
        public void close() {
            synchronized (activeProgress) {
                activeProgress.remove(impl);
            }
        }
    }

}
