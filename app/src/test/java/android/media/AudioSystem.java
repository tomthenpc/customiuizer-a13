package android.media;

import java.util.Collections;
import java.util.Set;

public class AudioSystem {
    public static final int DEVICE_OUT_DEFAULT = 2;
    public static final Set<Integer> DEVICE_OUT_ALL_SET = Collections.unmodifiableSet(
        new java.util.LinkedHashSet<>(java.util.Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024))
    );
    public static final int[] DEFAULT_STREAM_VOLUME = new int[]{
        1, 5, 5, 5, 5, 5, 7, 5, 5, 5
    };
}
