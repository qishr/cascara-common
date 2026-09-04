// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.color;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.util.StringUtils;

public class ColorUtils {

    private ColorUtils() {
        // No public constructor
    }

    public static String toRgbHex(RgbaColor color) {
        return String.format(
            "#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255)
        );
    }

    public static String toRgbaHex(RgbaColor color) {
        if (color.alpha * 255 > 254.5) {
            return toRgbHex(color);
        }
        String hex = String.format(
            "#%02X%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255),
            (int) (color.getAlpha() * 255)
        );
        return hex;
    }

    public static String toRgbHex(HsbaColor color) {
        return toRgbaHex(toRgbaColor(color));
    }

    public static String toRgbaHex(HsbaColor color) {
        return toRgbaHex(toRgbaColor(color));
    }

    public static RgbaColor toRgbaColor(String colorString) {
        if (toColor(colorString) instanceof RgbaColor color) {
            return color;
        }
        throw new ColorException(ColorDiagnosticCode.INVALID_COLOR_FORMAT, colorString);
    }

    public static HsbaColor toHsbaColor(String colorString) {
        ColorPrimitive color = toColor(colorString);
        if (color instanceof HsbaColor hsbaColor) {
            return hsbaColor;
        } else if (color instanceof RgbaColor rgbaColor) {
            HsbaColor hsbaColor = toHsbaColor(rgbaColor);
            return hsbaColor;
        }
        throw new ColorException(ColorDiagnosticCode.INVALID_COLOR_FORMAT, colorString);
    }

    public static ColorPrimitive toColor(String colorString) {
        if (colorString == null) {
            throw new LocalizableRuntimeException(GenericDiagnosticCode.UNEXPECTED_NULL_PARAMETER, "ColorUtils", "toColor");
        }
        if (colorString.isEmpty()) {
            throw new LocalizableRuntimeException(ColorDiagnosticCode.INVALID_COLOR_FORMAT, colorString);
        }
        colorString = colorString.toLowerCase(Locale.ROOT);
        ColorPrimitive color = null;
        if (colorString.startsWith("#") || colorString.startsWith("0x")) {
            color = RgbaColor.from(colorString);
        } else if (colorString.startsWith("rgba(")) {
            color = toRgbaColor(StringUtils.parseNumberList(stripCloseParen(colorString.substring(5))));
        } else if (colorString.startsWith("rgb(")) {
            color = toRgbaColor(StringUtils.parseNumberList(stripCloseParen(colorString.substring(4))));
        } else if (colorString.startsWith("hsba(")) {
            color = toHsbaColor(StringUtils.parseNumberList(stripCloseParen(colorString.substring(5))));
        } else if (colorString.startsWith("hsl(")) {
            color = toHsbaColor(StringUtils.parseNumberList(stripCloseParen(colorString.substring(4))));
        }
        if (color == null) {
            throw new ColorException(ColorDiagnosticCode.INVALID_COLOR_FORMAT, colorString);
        }
        return color;
    }

    public static RgbaColor toRgbaColor(List<Number> components) {
        if (components.size() < 3) {
            return null;
        } else {
            if (components.getFirst() instanceof Double) {
                double red = components.get(0).doubleValue();
                double green = components.get(1).doubleValue();
                double blue = components.get(2).doubleValue();
                double alpha = components.size() > 3
                    ? components.get(3).doubleValue()
                    : 1;
                return new RgbaColor(red, green, blue, alpha);
            } else {
                int red = components.get(0).intValue();
                int green = components.get(1).intValue();
                int blue = components.get(2).intValue();
                double alpha = components.size() > 3
                    ? components.get(3).doubleValue()
                    : 1;
                return new RgbaColor(red, green, blue, alpha);
            }
        }
    }

    public static HsbaColor toHsbaColor(List<Number> components) {
        if (components.size() < 3) {
            return null;
        } else {
            double hue = components.get(0).doubleValue();
            double saturation = components.get(1).doubleValue();
            double brightness = components.get(2).doubleValue();
            double alpha = components.size() > 3
                ? components.get(3).doubleValue()
                : 1;
            return new HsbaColor(hue, saturation, brightness, alpha);
        }
    }

    public static HsbaColor toHsbaColor(RgbaColor color) {
        return toHsbaColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static RgbaColor toRgbaColor(HsbaColor color) {
        return toRgbaColor(color.getHue(), color.getSaturation(), color.getBrightness(), color.getAlpha());
    }

    public static RgbaColor toRgbaColor(double hue, double saturation, double brightness, double alpha) {
        // normalize the hue
        double normalizedHue = ((hue % 360) + 360) % 360;
        hue = normalizedHue/360;

        double r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = brightness;
        } else {
            double h = (hue - Math.floor(hue)) * 6.0;
            double f = h - Math.floor(h);
            double p = brightness * (1.0 - saturation);
            double q = brightness * (1.0 - saturation * f);
            double t = brightness * (1.0 - (saturation * (1.0 - f)));
            switch ((int) h) {
                case 0:
                    r = brightness;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = brightness;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = brightness;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = brightness;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = brightness;
                    break;
                case 5:
                    r = brightness;
                    g = p;
                    b = q;
                    break;
            }
        }
        return new RgbaColor(r, g, b, alpha);
    }

    public static HsbaColor toHsbaColor(double r, double g, double b, double alpha) {
        double hue, saturation, brightness;

        double cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        double cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = cmax;
        if (cmax != 0)
            saturation = (cmax - cmin) / cmax;
        else
            saturation = 0;

        if (saturation == 0) {
            hue = 0;
        } else {
            double redc = (cmax - r) / (cmax - cmin);
            double greenc = (cmax - g) / (cmax - cmin);
            double bluec = (cmax - b) / (cmax - cmin);
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0 + redc - bluec;
            else
                hue = 4.0 + greenc - redc;
            hue = hue / 6.0;
            if (hue < 0)
                hue = hue + 1.0;
        }
        return new HsbaColor(hue * 360, saturation, brightness, alpha);
    }

    /// RGB interpolation
    public static RgbaColor lerp(RgbaColor a, RgbaColor b, double t) {
        t = Math.clamp(t, 0, 1);
        double red = lerp(a.getRed(), b.getRed(), t);
        double blue = lerp(a.getBlue(), b.getBlue(), t);
        double green = lerp(a.getGreen(), b.getGreen(), t);
        double alpha = lerp(a.getAlpha(), b.getAlpha(), t);
        RgbaColor color = new RgbaColor(red, green, blue, alpha);
        return color;
    }

    /// Hue interpolation
    public static HsbaColor lerp(HsbaColor a, HsbaColor b, double t) {
        double dh;
        if (b.hue > a.hue && b.hue - a.hue > 180) {
            dh = b.hue - (a.hue + 360);
        } else if (b.hue < a.hue && a.hue - b.hue > 180) {
            dh = b.hue + 360 - a.hue;
        } else {
            dh = b.hue - a.hue;
        }
        double hue = a.hue + t * dh;
        double sat = lerp(a.saturation, b.saturation, t);
        double bri = lerp(a.brightness, b.brightness, t);
        double alp = lerp(a.alpha, b.alpha, t);
        HsbaColor color = new HsbaColor(hue, sat, bri, alp);
        return color;
    }

    public static void processColor(ColorDefinition color) throws ColorException {
        processColor(color, new Colors(null, null, null));
    }

    public static void processColor(
        ColorDefinition color,
        Map<String,ColorDefinition> baseColors,
        Map<String,ColorDefinition> paletteColors,
        Map<String,ColorDefinition> transforms
    ) throws ColorException {
        processColor(color, new Colors(baseColors, paletteColors, transforms));
    }

    public static void copy(ColorPrimitive from, ColorPrimitive to) {
        if (from instanceof RgbaColor fromRgba) {
            if (to instanceof RgbaColor toRgba) {
                toRgba.red = fromRgba.red;
                toRgba.green = fromRgba.green;
                toRgba.blue = fromRgba.blue;
                toRgba.alpha = fromRgba.alpha;
            } else if (to instanceof HsbaColor toHsba) {
                HsbaColor color = toHsbaColor(fromRgba);
                copy(color, toHsba);
            }
        } else if (from instanceof HsbaColor fromHsba) {
            if (to instanceof RgbaColor toRgba) {
                RgbaColor color = toRgbaColor(fromHsba);
                copy(color, toRgba);
            } else if (to instanceof HsbaColor toHsba) {
                toHsba.hue = fromHsba.hue;
                toHsba.saturation = fromHsba.saturation;
                toHsba.brightness = fromHsba.brightness;
                toHsba.alpha = fromHsba.alpha;
            }
        }
    }

    // public static void copy(RgbaColor from, RgbaColor to) {
    //     to.red = from.red;
    //     to.green = from.green;
    //     to.blue = from.blue;
    //     to.alpha = from.alpha;
    // }

    // public static void copy(HsbaColor from, HsbaColor to) {
    //     to.hue = from.hue;
    //     to.saturation = from.saturation;
    //     to.brightness = from.brightness;
    //     to.alpha = from.alpha;
    // }

    // public static void copy(RgbaColor from, HsbaColor to) {
    //     HsbaColor color = toHsbaColor(from);
    //     copy(color, to);
    // }

    // public static void copy(HsbaColor from, RgbaColor to) {
    //     RgbaColor color = toRgbaColor(from);
    //     copy(color, to);
    // }

    //
    // Private Helpers
    //

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // private static double max(double a, double b, double c) {
    //     return Math.max(Math.max(a,b), c);
    // }

    private static String stripCloseParen(String string) {
        int pos = string.indexOf(")");
        if (pos > -1) {
            return string.substring(0, pos - 1);
        }
        return string;
    }

    private static void processColor(ColorDefinition color, Colors colors) throws ColorException {
        if (color.usesTransform()) {
            applyTransform(color, colors);
        } else if (color.usesPaletteColor()) {
            lookupPaletteColor(color, colors);
        } else if (color.usesBaseColor()) {
            lookupBaseColor(color, colors);
        } else if (color.usesLerp()) {
            applyLerp(color);
        }
    }

    private static void applyTransform(ColorDefinition definition, Colors colors) throws ColorException {
        String transformId = definition.getTransformId();
        if (transformId == null) {
            System.out.println("transformId = null");
            return;
        }

        if (colors.getTransforms() == null) {
            return;
        }

        ColorDefinition transformDefinition = colors.getTransforms().get(transformId);
        if (transformDefinition == null) {
            System.out.println("transformDefinition = null");
            return;
        }

        String baseColorId = definition.getBaseColorId();
        if (baseColorId == null) {
            System.out.println("baseColorId = null");
            return;
        }

        String transform = transformDefinition.getTransformDefinition();
        if (transform == null) {
            System.out.println("transform = null");
            return;
        }

        if (colors.getBaseColors() == null) {
            return;
        }

        ColorDefinition baseColorDef = colors.getBaseColors().get(baseColorId);
        if (baseColorDef == null) {
            System.out.println("baseColorDef = null");
            return;
        }

        RgbaColor baseColor = toRgbaColor(baseColorDef.getHexColor());

        int colon = transform.indexOf(":");
        if (colon == -1) {
            return;
        }
        String transformFunction = transform.substring(0, colon).toLowerCase();
        String transformParamString = transform.substring(colon + 1);
        double transformParam = Double.parseDouble(transformParamString);

        RgbaColor result;
        if (transformFunction.equals("brightness")) {
            result = brightnessFunc(baseColor, transformParam);
        } else if (transformFunction.equals("saturation")) {
            result = saturationFunc(baseColor, transformParam);
        } else {
            return;
        }

        definition.setHexColor(toRgbHex(result));
    }


    // private static void lookupColor(ColorDefinition definition, Map<String,ColorDefinition> colorMap) throws ColorException {
    //     ColorDefinition paletteColor = colorMap.get(definition.getPaletteColorId());
    //     if (paletteColor != null) {
    //         processColor(paletteColor, variation);
    //         definition.setHexColor(paletteColor.getHexColor());
    //     }
    // }

    /// Gets the theme variation's palette color definition that matches the
    /// specified definition, then processes it and sets the specified
    /// definition's hex color to the result.
    private static void lookupPaletteColor(ColorDefinition definition, Colors colors) throws ColorException {
        ColorDefinition paletteColor = colors.getPaletteColors().get(definition.getPaletteColorId());
        if (paletteColor != null) {
            processColor(paletteColor, colors);
            definition.setHexColor(paletteColor.getHexColor());
        }
    }

    /// Gets the theme variation's base color definition that matches the
    /// specified definition, then processes it and sets the specified
    /// definition's hex color to the result.
    private static void lookupBaseColor(ColorDefinition definition, Colors colors) throws ColorException {
        ColorDefinition baseColor = colors.getBaseColors().get(definition.getBaseColorId());
        if (baseColor != null) {
            processColor(baseColor, colors);
            definition.setHexColor(baseColor.getHexColor());
        }
    }

    private static void applyLerp(ColorDefinition definition) throws ColorException {
        double d = Double.parseDouble(definition.getLerp());

        String leftHex = definition.getLeftHexColor();
        RgbaColor left = toRgbaColor(leftHex.isEmpty() ? definition.getHexColor() : leftHex);

        String rightHex = definition.getRightHexColor();
        RgbaColor right = toRgbaColor(rightHex.isEmpty() ? definition.getHexColor() : rightHex);

        RgbaColor interpolated = lerp(left, right, d);
        definition.setHexColor(toRgbHex(interpolated));
    }

    private static RgbaColor brightnessFunc(RgbaColor c, double value) {
        return c.brighter().deriveColor(0, 1.0, 1.0 + value, 1.0);
    }

    private static RgbaColor saturationFunc(RgbaColor c, double value) {
        return c.saturate().deriveColor(0, 1.0 + value, 1.0, 1.0);
    }

    private static class Colors {
        Map<String,ColorDefinition> baseColors = new HashMap<>();
        Map<String,ColorDefinition> paletteColors = new HashMap<>();
        Map<String,ColorDefinition> transforms = new HashMap<>();

        public Colors(Map<String,ColorDefinition> baseColors, Map<String,ColorDefinition> paletteColors, Map<String,ColorDefinition> transforms) {
            this.baseColors = baseColors;
            this.paletteColors = paletteColors;
            this.transforms = transforms;
        }

        public Map<String,ColorDefinition> getBaseColors() {
            return baseColors;
        }

        public Map<String,ColorDefinition> getPaletteColors() {
            return paletteColors;
        }

        public Map<String,ColorDefinition> getTransforms() {
            return transforms;
        }
    }
}
