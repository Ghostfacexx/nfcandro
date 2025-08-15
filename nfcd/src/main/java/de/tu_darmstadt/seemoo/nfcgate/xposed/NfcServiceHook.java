package de.tu_darmstadt.seemoo.nfcgate.xposed;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.RemoteException;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;
import java.util.HashMap;
public class NfcServiceHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook only for the system process
        if (!lpparam.packageName.equals("android")) {
            return;
        }
        try {
            // Android 16+ NFC service package
            Class<?> nfcServiceClass = XposedHelpers.findClass(
                    "com.google.android.nfc.NfcService", lpparam.classLoader);
            // Hook the enable() method
            XposedHelpers.findAndHookMethod(
                    nfcServiceClass,
                    "enable",
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Force NFC enable
                            param.args[0] = true;
                        }
                    }
            );
            // Hook the disable() method
            XposedHelpers.findAndHookMethod(
                    nfcServiceClass,
                    "disable",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Force NFC disable
                            param.setResult(null);
                        }
                    }
            );
            // Hook the dispatchIntent() method
            XposedHelpers.findAndHookMethod(
                    nfcServiceClass,
                    "dispatchIntent",
                    Intent.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Modify intents before they are sent
                            Intent intent = (Intent) param.args[0];
                            // Example: add extra data
                            intent.putExtra("custom_data", "value");
                        }
                    }
            );
        } catch (XposedHelpers.ClassNotFoundError e) {
            // Android <16 fallback
            Class<?> nfcServiceClass = XposedHelpers.findClass(
                    "android.nfc.NfcService", lpparam.classLoader);
            // Hook methods similarly
        }
    }
}