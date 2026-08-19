package com.lbe.security.ui;

import android.content.Context;

/**
 * Test fixture so B2A-D1 can prove the ClipboardTipDialog sub-hook remains
 * installed when {@code SecurityPromptHandler} is missing.
 */
public final class ClipboardTipDialog {
    private ClipboardTipDialog() {}

    public static boolean customReadClipboardDialog(Context context, String text) {
        return true;
    }
}
