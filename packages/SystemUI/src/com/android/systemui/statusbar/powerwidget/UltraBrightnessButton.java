package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.os.SystemProperties;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

public class UltraBrightnessButton extends PowerButton {

    private static final String ULTRA_BRIGHTNESS_PERSIST_PROP = "persist.sys.ultrabrightness";

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();
    static {
        OBSERVED_URIS.add(Settings.System.getUriFor(Settings.System.ACHEP_ULTRA_BRIGHTNESS));
    }

    public UltraBrightnessButton() { mType = BUTTON_ULTRA_BRIGHTNESS; }

    @Override
    protected void updateState(Context context) {
        if (getOrientationState(context) == 1) {
            mIcon = R.drawable.stat_ultra_brightness_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_ultra_brightness_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState(Context context) {
        if (getOrientationState(context) == 0) {
            SystemProperties.set(ULTRA_BRIGHTNESS_PERSIST_PROP, "1");
            writeOneLine("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/mode", "i2c_pwm");
            Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.ACHEP_ULTRA_BRIGHTNESS, 1);
        } else {
            SystemProperties.set(ULTRA_BRIGHTNESS_PERSIST_PROP, "0");
            writeOneLine("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/mode", "i2c_pwm_als");
            Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.ACHEP_ULTRA_BRIGHTNESS, 0);
        }
    }


    @Override
    protected boolean handleLongClick(Context context) {
        Intent intent = new Intent("android.settings.DISPLAY_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }

    @Override
    protected List<Uri> getObservedUris() {
        return OBSERVED_URIS;
    }

    private int getOrientationState(Context context) {
        return Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.ACHEP_ULTRA_BRIGHTNESS, 0);
    }
	
	public static boolean writeOneLine(String fname, String value) {
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
