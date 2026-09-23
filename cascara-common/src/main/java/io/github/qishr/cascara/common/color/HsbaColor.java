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

public final class HsbaColor implements ColorPrimitive, Duplicable<HsbaColor> {
    public double hue;
    public double saturation;
    public double brightness;
    public double alpha;

    public HsbaColor(double hue, double saturation, double brightness, double alpha) {
        validate(saturation, brightness);
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
        this.alpha = alpha;
    }

    public String toString() {
        return String.format(
            "%s, %s, %s, %s",
            StringUtils.fromDouble(hue, 3),
            StringUtils.fromDouble(saturation, 3),
            StringUtils.fromDouble(brightness, 3),
            StringUtils.fromDouble(alpha, 3)
        );
    }

    public double getHue() { return hue; }
    public double getSaturation() { return saturation; }
    public double getBrightness() { return brightness; }

    @Override
    public double getAlpha() { return alpha; }

    public HsbaColor setHue(double hue) {
        this.hue = hue;
        return this;
    }

    public HsbaColor setSaturation(double saturation) {
        this.saturation = saturation;
        return this;
    }

    public HsbaColor setBrightness(double brightness) {
        this.brightness = Math.min(1.0, brightness);
        return this;
    }

    @Override
    public HsbaColor setAlpha(double alpha) {
        this.alpha = alpha;
        return this;
    }

    //
    //
    //

    @Override
    public HsbaColor brighter() {
        return deriveColor(0, 1.0, 1.0 / BRIGHTNESS_FACTOR, 1.0);
    }

    @Override
    public HsbaColor saturate() {
        return deriveColor(0, 1.0 / SATURATION_FACTOR, 1.0, 1.0);
    }

    @Override
    public HsbaColor deriveColor(double hueShift, double saturationFactor, double brightnessFactor, double opacityFactor) {
        double h = (((hue + hueShift) % 360) + 360) % 360;
        double s = Math.max(Math.min(saturation * saturationFactor, 1.0), 0.0);
        double b = Math.max(Math.min(brightness * brightnessFactor, 1.0), 0.0);
        double a = Math.max(Math.min(alpha * opacityFactor, 1.0), 0.0);
        return new HsbaColor(h, s, b, a);
    }

    @Override
    public HsbaColor duplicate() {
        return new HsbaColor(hue, saturation, brightness, alpha);
    }

    //
    //
    //

    private static void validate(double saturation, double brightness) {
        if (saturation < 0.0 || saturation > 1.0) {
            throw new IllegalArgumentException("Color.hsb's saturation parameter (" + saturation + ") expects values 0.0-1.0");
        }
        if (brightness < 0.0 || brightness > 1.0) {
            throw new IllegalArgumentException("Color.hsb's brightness parameter (" + brightness + ") expects values 0.0-1.0");
        }
    }
}
