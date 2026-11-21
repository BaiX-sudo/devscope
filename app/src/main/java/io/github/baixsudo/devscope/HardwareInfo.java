package io.github.baixsudo.devscope;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Collects device, hardware and system information into ordered sections. */
public class HardwareInfo {

    public static class Item {
        public final String key;
        public final String value;
        Item(String key, String value) { this.key = key; this.value = value; }
    }

    public static class Section {
        public final String title;
        public final List<Item> items = new ArrayList<>();
        Section(String title) { this.title = title; }
        void add(String key, Object value) {
            items.add(new Item(key, value == null ? "-" : String.valueOf(value)));
        }
    }

    public static List<Section> gather(Context ctx) {
        List<Section> sections = new ArrayList<>();
        sections.add(device(ctx));
        sections.add(system(ctx));
        sections.add(cpu(ctx));
        sections.add(memory(ctx));
        sections.add(storage(ctx));
        sections.add(battery(ctx));
        sections.add(display(ctx));
        sections.add(gpu(ctx));
        sections.add(network(ctx));
        sections.add(sensors(ctx));
        return sections;
    }

    private static Section device(Context ctx) {
        Section s = new Section("Device");
        s.add("Manufacturer", Build.MANUFACTURER);
        s.add("Brand", Build.BRAND);
        s.add("Model", Build.MODEL);
        s.add("Device", Build.DEVICE);
        s.add("Product", Build.PRODUCT);
        s.add("Board", Build.BOARD);
        s.add("Hardware", Build.HARDWARE);
        return s;
    }

    private static Section system(Context ctx) {
        Section s = new Section("System");
        s.add("Android version", Build.VERSION.RELEASE);
        s.add("API level", Build.VERSION.SDK_INT);
        s.add("Build ID", Build.DISPLAY);
        s.add("Security patch", Build.VERSION.SECURITY_PATCH);
        s.add("Kernel", System.getProperty("os.version"));
        s.add("Architecture", System.getProperty("os.arch"));
        s.add("Java VM", System.getProperty("java.vm.version"));
        s.add("Boot time", formatUptime(System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()));
        s.add("Rooted", isRooted() ? "Yes (likely)" : "No");
        return s;
    }

    private static Section cpu(Context ctx) {
        Section s = new Section("CPU");
        if (Build.VERSION.SDK_INT >= 31) {
            s.add("SoC", (Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL).trim());
        }
        s.add("ABIs", String.join(", ", Build.SUPPORTED_ABIS));
        s.add("Cores", Runtime.getRuntime().availableProcessors());
        s.add("Min frequency", cpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"));
        s.add("Max frequency", cpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
        s.add("Governor", readFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"));
        return s;
    }

    private static Section memory(Context ctx) {
        Section s = new Section("Memory");
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        s.add("Total RAM", formatBytes(mi.totalMem));
        s.add("Available", formatBytes(mi.availMem));
        s.add("Low memory", mi.lowMemory ? "Yes" : "No");
        s.add("Threshold", formatBytes(mi.threshold));
        Runtime rt = Runtime.getRuntime();
        s.add("Heap max", formatBytes(rt.maxMemory()));
        s.add("Heap used", formatBytes(rt.totalMemory() - rt.freeMemory()));
        return s;
    }

    private static Section storage(Context ctx) {
        Section s = new Section("Storage");
        addStatFs(s, "Internal", Environment.getDataDirectory());
        addStatFs(s, "External", Environment.getExternalStorageDirectory());
        return s;
    }

    private static void addStatFs(Section s, String label, File path) {
        try {
            StatFs sf = new StatFs(path.getAbsolutePath());
            long total = sf.getTotalBytes();
            long free = sf.getAvailableBytes();
            s.add(label + " total", formatBytes(total));
            s.add(label + " free", formatBytes(free) + " (" + percent(free, total) + ")");
        } catch (Exception e) {
            s.add(label, "unavailable");
        }
    }

    private static Section battery(Context ctx) {
        Section s = new Section("Battery");
        Intent st = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (st == null) { s.add("Status", "unknown"); return s; }
        int level = st.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = st.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        s.add("Level", level >= 0 && scale > 0 ? (level * 100 / scale) + "%" : "unknown");
        s.add("Status", batteryStatus(st.getIntExtra(BatteryManager.EXTRA_STATUS, -1)));
        s.add("Health", batteryHealth(st.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)));
        s.add("Technology", st.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
        int temp = st.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        s.add("Temperature", temp > 0 ? (temp / 10f) + " \u00b0C" : "unknown");
        int volt = st.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        s.add("Voltage", volt > 0 ? (volt / 1000f) + " V" : "unknown");
        s.add("Power source", batteryPlugged(st.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)));
        return s;
    }

    private static Section display(Context ctx) {
        Section s = new Section("Display");
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        d.getRealMetrics(dm);
        s.add("Resolution", dm.widthPixels + " \u00d7 " + dm.heightPixels);
        s.add("Density", dm.densityDpi + " dpi (" + densityBucket(dm.densityDpi) + ")");
        s.add("Scaled density", dm.scaledDensity);
        float refresh = 0;
        try { refresh = d.getRefreshRate(); } catch (Exception ignored) {}
        s.add("Refresh rate", refresh > 0 ? refresh + " Hz" : "unknown");
        s.add("Screen size", (Math.round(dm.widthPixels / dm.xdpi * 10) / 10f) + "\" (approx.)");
        return s;
    }

    private static Section gpu(Context ctx) {
        Section s = new Section("GPU");
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            String gl = am.getDeviceConfigurationInfo().getGlEsVersion();
            s.add("OpenGL ES", gl);
        } catch (Exception e) {
            s.add("OpenGL ES", "unknown");
        }
        s.add("Renderer", readFile("/sys/class/graphics/fb0/name"));
        return s;
    }

    private static Section network(Context ctx) {
        Section s = new Section("Network");
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        String type = "unknown";
        try {
            Network nw = cm.getActiveNetwork();
            NetworkCapabilities caps = cm.getNetworkCapabilities(nw);
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "Wi-Fi";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "Cellular";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Ethernet";
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) type = "Bluetooth";
            }
        } catch (Exception ignored) {}
        s.add("Connected", cm.getActiveNetwork() != null ? "Yes" : "No");
        s.add("Transport", type);
        for (String ip : ipAddresses()) {
            s.add("IP address", ip);
        }
        return s;
    }

    private static Section sensors(Context ctx) {
        Section s = new Section("Sensors");
        SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
        if (sensors.isEmpty()) { s.add("Sensors", "none"); return s; }
        for (Sensor sensor : sensors) {
            s.add(sensor.getName(), sensor.getVendor() + " \u00b7 " + sensor.getType());
        }
        return s;
    }

    // ---- helpers ----

    private static String cpuFreq(String path) {
        String v = readFile(path);
        if (v == null) return "unknown";
        try {
            long khz = Long.parseLong(v.trim());
            return (khz / 1000f) + " MHz";
        } catch (NumberFormatException e) {
            return "unknown";
        }
    }

    private static String readFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists() || !f.canRead()) return null;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            br.close();
            return line == null ? null : line.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isRooted() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
                "/system/app/Superuser.apk", "/data/local/xbin/su"};
        for (String p : paths) if (new File(p).exists()) return true;
        String tags = Build.TAGS;
        return tags != null && tags.contains("test-keys");
    }

    private static List<String> ipAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (java.net.InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        out.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static String batteryStatus(int v) {
        switch (v) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not charging";
            default: return "Unknown";
        }
    }

    private static String batteryHealth(int v) {
        switch (v) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over voltage";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            default: return "Unknown";
        }
    }

    private static String batteryPlugged(int v) {
        switch (v) {
            case BatteryManager.BATTERY_PLUGGED_AC: return "AC";
            case BatteryManager.BATTERY_PLUGGED_USB: return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: return "Wireless";
            default: return "Battery";
        }
    }

    private static String densityBucket(int dpi) {
        if (dpi <= 120) return "ldpi";
        if (dpi <= 160) return "mdpi";
        if (dpi <= 240) return "hdpi";
        if (dpi <= 320) return "xhdpi";
        if (dpi <= 480) return "xxhdpi";
        if (dpi <= 640) return "xxxhdpi";
        return "unknown";
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        double v = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
        return String.format(Locale.US, i == 0 ? "%.0f %s" : "%.1f %s", v, units[i]);
    }

    private static String percent(long part, long total) {
        if (total <= 0) return "0%";
        return String.format(Locale.US, "%.0f%% free", 100.0 * part / total);
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }
}
