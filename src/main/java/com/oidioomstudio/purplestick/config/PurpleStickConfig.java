package com.oidioomstudio.purplestick.config;

import com.oidioomstudio.olib.config.OConfigHolder;

public class PurpleStickConfig {
    public static class Data {
        public int maxBlocks = 1024;
        public boolean durabilityEnabled = true;
        public boolean soundEnabled = true;
        public int undoHistoryLimit = 10;
        public boolean renderPreview = true;
        public int outlineColor = 0xFFA855F7; // Vibrant Purple
    }

    private static final OConfigHolder<Data> HOLDER = new OConfigHolder<>(
            "purplestick",
            Data.class,
            Data::new
    );

    private static Data current = new Data();

    public static void load() {
        current = HOLDER.load();
    }

    public static void save() {
        HOLDER.save();
    }

    public static Data get() {
        if (current == null) {
            load();
        }
        return current;
    }
}
