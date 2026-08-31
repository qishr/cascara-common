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

import io.github.qishr.cascara.common.util.Duplicable;
import io.github.qishr.cascara.common.util.StringUtils;

public final class RgbaColor implements ColorPrimitive, Duplicable<RgbaColor> {
    public double red;
    public double green;
    public double blue;
    public double alpha;

    public RgbaColor(double red, double green, double blue, double alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public RgbaColor(int red, int green, int blue, double alpha) {
        validate(red, green, blue);
        this.red = red / 255.0;
        this.green = green / 255.0;
        this.blue = blue / 255.0;
        this.alpha = alpha;
    }

    public static RgbaColor from(String hexColor) {
        return parseHex(hexColor);
    }

    public RgbaColor setHexColor(String hexColor) {
        RgbaColor color = parseHex(hexColor);
        ColorUtils.copy(color, this);
        return this;
    }

    public String toString() {
        return String.format(
            "%s, %s, %s, %s",
            StringUtils.fromDouble(red, 3),
            StringUtils.fromDouble(green, 3),
            StringUtils.fromDouble(blue, 3),
            StringUtils.fromDouble(alpha, 3)
        );
    }

    public double getRed() { return red; }
    public double getGreen() { return green; }
    public double getBlue() { return blue; }

    @Override
    public double getAlpha() { return alpha; }

    public RgbaColor setRed(double red) {
        this.red = red;
        return this;
    }

    public RgbaColor setGreen(double green) {
        this.green = green;
        return this;
    }

    public RgbaColor setBlue(double blue) {
        this.blue = blue;
        return this;
    }

    @Override
    public RgbaColor setAlpha(double alpha) {
        this.alpha = alpha;
        return this;
    }

    //
    //
    //

    @Override
    public RgbaColor brighter() {
        return deriveColor(0, 1.0, 1.0 / BRIGHTNESS_FACTOR, 1.0);
    }

    @Override
    public RgbaColor saturate() {
        return deriveColor(0, 1.0 / SATURATION_FACTOR, 1.0, 1.0);
    }

    @Override
    public RgbaColor deriveColor(double hueShift, double saturationFactor,
                             double brightnessFactor, double opacityFactor) {

        HsbaColor color = ColorUtils.toHsbaColor(this);

        /* Allow brightness increase of black color */
        double b = color.getBrightness();
        if (b == 0 && brightnessFactor > 1.0) {
            b = 0.05;
        }

        double h = (((color.getHue() + hueShift) % 360) + 360) % 360;
        double s = Math.max(Math.min(color.getSaturation() * saturationFactor, 1.0), 0.0);
        b = Math.max(Math.min(b * brightnessFactor, 1.0), 0.0);
        double a = Math.max(Math.min(alpha * opacityFactor, 1.0), 0.0);
        return ColorUtils.toRgbaColor(h, s, b, a);
    }

    @Override
    public RgbaColor duplicate() {
        return new RgbaColor(red, green, blue, alpha);
    }

    //
    //
    //

    private static RgbaColor parseHex(String color) {
        if (color.startsWith("#")) {
            color = color.substring(1);
        } else if (color.startsWith("0x")) {
            color = color.substring(2);
        }

        int len = color.length();
        double alpha = 1;

        try {
            int r;
            int g;
            int b;
            int a;

            if (len == 3) {
                r = Integer.parseInt(color.substring(0, 1), 16);
                g = Integer.parseInt(color.substring(1, 2), 16);
                b = Integer.parseInt(color.substring(2, 3), 16);
                return new RgbaColor(r / 15.0, g / 15.0, b / 15.0, alpha);
            } else if (len == 4) {
                r = Integer.parseInt(color.substring(0, 1), 16);
                g = Integer.parseInt(color.substring(1, 2), 16);
                b = Integer.parseInt(color.substring(2, 3), 16);
                a = Integer.parseInt(color.substring(3, 4), 16);
                return new RgbaColor(r / 15.0, g / 15.0, b / 15.0,
                        alpha * a / 15.0);
            } else if (len == 6) {
                r = Integer.parseInt(color.substring(0, 2), 16);
                g = Integer.parseInt(color.substring(2, 4), 16);
                b = Integer.parseInt(color.substring(4, 6), 16);
                return new RgbaColor(r, g, b, alpha);
            } else if (len == 8) {
                r = Integer.parseInt(color.substring(0, 2), 16);
                g = Integer.parseInt(color.substring(2, 4), 16);
                b = Integer.parseInt(color.substring(4, 6), 16);
                a = Integer.parseInt(color.substring(6, 8), 16);
                return new RgbaColor(r, g, b, alpha * a / 255.0);
            }
        } catch (NumberFormatException nfe) {}

        throw new IllegalArgumentException("Invalid color specification");
    }

    private static void validate(int red, int green, int blue) {
        if (red < 0 || red > 255) {
            throw new ColorException(ColorDiagnosticCode.RED_RANGE, red);
        }
        if (green < 0 || green > 255) {
            throw new ColorException(ColorDiagnosticCode.BLUE_RANGE, red);
        }
        if (blue < 0 || blue > 255) {
            throw new ColorException(ColorDiagnosticCode.GREEN_RANGE, red);
        }
    }
}
