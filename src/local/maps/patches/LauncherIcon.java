// SPDX-License-Identifier: GPL-3.0-only
package local.maps.patches;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import app.morphe.patcher.patch.ResourcePatchContext;

/**
 * Patches the launcher (home screen) adaptive icon for Google Maps Automotive:
 * 1. Locates all adaptive icon background definitions (colors.xml and background drawables).
 * 2. Sets the background color to pure black (#000000) and creates a dark background shape
 *    with a subtle dark gray border (#1F2124), identical to the Morphe / YouTube dark launcher style.
 * 3. In ic_launcher_foreground.xml, converts any white fill layer inside the Maps pin's center
 *    hole to black (#000000) so the center hole seamlessly matches the black background.
 */
final class LauncherIcon {
    private static final String DARK_BG_SHAPE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        + "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
        + "    android:shape=\"rectangle\">\n"
        + "    <solid android:color=\"#000000\" />\n"
        + "    <stroke\n"
        + "        android:width=\"1.5dp\"\n"
        + "        android:color=\"#1F2124\" />\n"
        + "</shape>\n";

    static void apply(ResourcePatchContext context) throws Exception {
        File res = context.get("res", false);
        if (res == null || !res.exists() || !res.isDirectory()) return;

        int colorsUpdated = patchValuesColors(res);
        int backgroundsUpdated = patchBackgroundDrawables(res);
        int adaptiveIconsUpdated = patchAdaptiveIconXmls(res);
        int foregroundsUpdated = patchForegroundVectors(res);

        System.out.println("Dark Launcher Icon: " + colorsUpdated + " colors updated, "
            + backgroundsUpdated + " background drawables written, "
            + adaptiveIconsUpdated + " adaptive icons updated, "
            + foregroundsUpdated + " foreground pins updated.");
    }

    private static int patchValuesColors(File res) throws Exception {
        int count = 0;
        File[] valuesDirs = res.listFiles(f -> f.isDirectory() && f.getName().startsWith("values"));
        if (valuesDirs == null) return 0;

        Pattern bgPattern = Pattern.compile("(?i)(<color\\s+name=[\"'](?:ic_)?launcher_background[\"']>)[^<]+(</color>)");

        for (File dir : valuesDirs) {
            File[] xmls = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".xml"));
            if (xmls == null) continue;
            for (File xml : xmls) {
                String content = Files.readString(xml.toPath(), StandardCharsets.UTF_8);
                Matcher m = bgPattern.matcher(content);
                if (m.find()) {
                    String updated = m.replaceAll("$1#000000$2");
                    Files.writeString(xml.toPath(), updated, StandardCharsets.UTF_8);
                    count++;
                }
            }
        }
        return count;
    }

    private static int patchBackgroundDrawables(File res) throws Exception {
        int count = 0;
        // Ensure res/drawable exists and write ic_launcher_background.xml
        File drawableDir = new File(res, "drawable");
        if (!drawableDir.exists()) drawableDir.mkdirs();
        File bgXml = new File(drawableDir, "ic_launcher_background.xml");
        Files.writeString(bgXml.toPath(), DARK_BG_SHAPE, StandardCharsets.UTF_8);
        count++;

        // Also check if other drawable-* directories already contain ic_launcher_background.xml
        File[] allDirs = res.listFiles(f -> f.isDirectory() && (f.getName().startsWith("drawable") || f.getName().startsWith("mipmap")));
        if (allDirs != null) {
            for (File d : allDirs) {
                File target = new File(d, "ic_launcher_background.xml");
                if (target.exists() && !target.equals(bgXml)) {
                    Files.writeString(target.toPath(), DARK_BG_SHAPE, StandardCharsets.UTF_8);
                    count++;
                }
            }
        }
        return count;
    }

    private static int patchAdaptiveIconXmls(File res) throws Exception {
        int count = 0;
        File[] dirs = res.listFiles(f -> f.isDirectory() && (f.getName().startsWith("mipmap") || f.getName().startsWith("drawable")));
        if (dirs == null) return 0;

        for (File d : dirs) {
            File[] files = d.listFiles(f -> f.isFile() && f.getName().endsWith(".xml") && f.getName().contains("launcher"));
            if (files == null) continue;
            for (File file : files) {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                if (content.contains("<adaptive-icon") && content.contains("<background")) {
                    String updated = content.replaceAll("(?i)(<background\\s+android:drawable=[\"'])@(android:)?color/white([\"'])",
                        "$1@drawable/ic_launcher_background$3");
                    if (!updated.equals(content)) {
                        Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int patchForegroundVectors(File res) throws Exception {
        int count = 0;
        File[] dirs = res.listFiles(f -> f.isDirectory() && (f.getName().startsWith("drawable") || f.getName().startsWith("mipmap")));
        if (dirs == null) return 0;

        Pattern whitePattern = Pattern.compile("(?i)(android:fillColor\\s*=\\s*[\"'])(?:#(?:fff|ffffff|ffffffff)|@(android:)?color/white)([\"'])");

        for (File d : dirs) {
            File[] files = d.listFiles(f -> f.isFile() && f.getName().endsWith(".xml") && f.getName().contains("foreground"));
            if (files == null) continue;
            for (File file : files) {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                Matcher m = whitePattern.matcher(content);
                if (m.find()) {
                    String updated = m.replaceAll("$1#000000$3");
                    Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
                    count++;
                }
            }
        }
        return count;
    }
}
