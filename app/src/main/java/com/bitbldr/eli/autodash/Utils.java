package com.bitbldr.eli.autodash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * General Utilites
 */
public final class Utils {
    private void Utils () {}

    /**
     * Starts a new activity using the given context and package name
     * @param context
     * @param packageName
     */
    public static void StartNewActivity(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Returns the string representation of number with leading zero if less than 10
     * @param number
     * @return
     */
    public static String DoubleDigitFormat(String number) {
        if (number.length() == 1) {
            return "0" + number;
        }

        return number;
    }

}
