package com.torretti.alarmsms;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * Locks the app behind the phone's fingerprint / face / screen lock.
 * Asked on first open and after more than one minute away from the app.
 */
final class AppLock {
    private static final long GRACE_MS = 60_000;
    private static final int REQ_CREDENTIAL = 4242;

    private static boolean unlocked = false;
    private static long leftAt = 0;
    private static boolean prompting = false;
    private static boolean warnedNoLock = false;

    private AppLock() {}

    static void leftApp() {
        if (unlocked) leftAt = SystemClock.elapsedRealtime();
    }

    static void require(Activity a) {
        if (unlocked && (leftAt == 0 || SystemClock.elapsedRealtime() - leftAt < GRACE_MS)) {
            leftAt = 0;
            show(a, true);
            return;
        }
        unlocked = false;
        if (prompting) { show(a, false); return; }

        KeyguardManager km = (KeyguardManager) a.getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) {
            // No screen lock on the phone: nothing to ask, but say so once.
            if (!warnedNoLock) {
                Toast.makeText(a, "Imposta un blocco schermo sul telefono per proteggere l'app",
                        Toast.LENGTH_LONG).show();
                warnedNoLock = true;
            }
            unlocked = true;
            show(a, true);
            return;
        }

        show(a, false);
        prompting = true;
        // wait until the window is on screen, otherwise the prompt can be cancelled at launch
        a.getWindow().getDecorView().post(() -> {
            try {
                if (Build.VERSION.SDK_INT >= 28) biometric(a, km);
                else credential(a, km);
            } catch (Exception e) {
                // never lock the user out because of a prompt problem: fall back to the phone PIN screen
                try { credential(a, km); } catch (Exception e2) { prompting = false; unlocked = true; show(a, true); }
            }
        });
    }

    private static void biometric(Activity a, KeyguardManager km) {
        BiometricPrompt.Builder b = new BiometricPrompt.Builder(a)
                .setTitle("Allarme Rolando")
                .setSubtitle("Sblocca con impronta o con il PIN del telefono");
        if (Build.VERSION.SDK_INT >= 30) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else {
            b.setDeviceCredentialAllowed(true);
        }
        b.build().authenticate(new CancellationSignal(), a.getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                        prompting = false;
                        unlocked = true;
                        leftAt = 0;
                        show(a, true);
                    }

                    @Override
                    public void onAuthenticationError(int code, CharSequence msg) {
                        if (code == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                                || code == BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON) {
                            prompting = false;
                            a.finishAffinity();       // user said no: close the app
                        } else {
                            // any other problem (cancelled by the system, sensor busy…): use the phone PIN screen
                            try { credential(a, km); }
                            catch (Exception e) { prompting = false; unlocked = true; show(a, true); }
                        }
                    }
                });
    }

    @SuppressWarnings("deprecation")
    private static void credential(Activity a, KeyguardManager km) {
        Intent i = km.createConfirmDeviceCredentialIntent("Allarme Rolando", "Sblocca con il PIN del telefono");
        if (i == null) { prompting = false; unlocked = true; show(a, true); return; }
        a.startActivityForResult(i, REQ_CREDENTIAL);
    }

    static void onResult(Activity a, int req, int res) {
        if (req != REQ_CREDENTIAL) return;
        prompting = false;
        if (res == Activity.RESULT_OK) {
            unlocked = true;
            leftAt = 0;
            show(a, true);
        } else {
            a.finishAffinity();
        }
    }

    /** Asks fingerprint / phone lock again before showing something sensitive. */
    static void confirm(Activity a, String why, Runnable onOk) {
        KeyguardManager km = (KeyguardManager) a.getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure() || Build.VERSION.SDK_INT < 28) { onOk.run(); return; }
        BiometricPrompt.Builder b = new BiometricPrompt.Builder(a)
                .setTitle("Allarme Rolando")
                .setSubtitle(why);
        if (Build.VERSION.SDK_INT >= 30) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else {
            b.setDeviceCredentialAllowed(true);
        }
        try {
            b.build().authenticate(new CancellationSignal(), a.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                            onOk.run();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(a, "Impossibile chiedere l'impronta: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Hides the screen content while the app is locked. */
    private static void show(Activity a, boolean visible) {
        a.getWindow().getDecorView().setAlpha(visible ? 1f : 0f);
    }
}
