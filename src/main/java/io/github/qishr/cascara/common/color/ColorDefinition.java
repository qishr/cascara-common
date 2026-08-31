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
import java.util.UUID;

import io.github.qishr.cascara.common.annotation.DataField;

public class ColorDefinition {
    private static final String NAME_PLACEHOLDER = "<name>";
    // public static final DataFormat DATA_FORMAT = new DataFormat("application/x-cascara-colordef");

    /// Automatically assigned ID of this color or transform function definition.
    @DataField
    String id = "";

    /// User-assigned name of the definition.
    @DataField
    String name = "";

    // @DataIgnore
    // int red;

    // @DataIgnore
    // int green;

    // @DataIgnore
    // int blue;

    // @DataIgnore
    // int opacity;

    /// 6 or 8 digit hex representation of the color after processing.
    ///
    /// This can be set by the user, although it will be updated
    /// automatically if any transformation/processing is defined.
    @DataField
    String hexColor = "";

    /// Hex representation of left color input to transform.
    ///
    /// If leftHexColor, rightHexColor, and lerp are defined, hexColor
    /// will be automatically updated.
    @DataField
    String leftHexColor = "";

    /// Hex representation of right color input to transform.
    ///
    /// If leftHexColor, rightHexColor, and lerp are defined, hexColor
    /// will be automatically updated.
    @DataField
    String rightHexColor = "";

    /// Lerp value when mixing leftHexColor and rightHexColor.
    ///
    /// If leftHexColor, rightHexColor, and lerp are defined, hexColor
    /// will be automatically updated.
    @DataField
    String lerp = "";

    /// ID of base color that either defines this color or is input to
    /// a transform function.
    ///
    /// If transformId is defined, a transform function will be applied and
    /// baseColorId will be used as an input.
    /// If transformId is underfined, hexColor will be automatically set
    /// to the color referred to by baseColorId, unless paletteColorId is
    /// also assigned.
    @DataField
    String baseColorId = "";

    /// ID of transform function to use.
    ///
    /// When defined, hexColor will be set to the output of the transformation
    /// that transformId refers to.
    @DataField
    String transformId = "";

    /// ID of palette color that this color is defined by.
    ///
    /// When defined, hexColor will be set to the palette color referred
    /// to by paletteColorId. This takes priority when baseColorId is not
    /// being used as input to a transform function.
    @DataField
    String paletteColorId = "";

    /// String representation of a transform function.
    @DataField
    String transformDefinition = "";

    /// Creates a new ColorDefinition with a unique ID and placeholder name.
    public ColorDefinition() {
        id = UUID.randomUUID().toString();
        name = NAME_PLACEHOLDER;
    }

    /// Creates a new ColorDefinition with a unique ID, placeholder name, and hex color code
    public ColorDefinition(String hexColor) {
        this();
        this.hexColor = hexColor;
    }

    public ColorDefinition(RgbaColor color) {
        this();
        this.hexColor = ColorUtils.toRgbaHex(color);
    }

    public ColorDefinition(HsbaColor color) {
        this();
        this.hexColor = ColorUtils.toRgbaHex(color);
    }

    /// Gets the unique ID of this color definition.
    public String getId() {
        return id;
    }

    /// Sets the unique ID of this color definition.
    /// @param id The ID string.
    public void setId(String id) {
        if (id == null) { throw new NullPointerException("Value must not be null"); }
        this.id = id;
    }

    /// Gets the user-assigned name of the definition.
    public String getName() {
        return name;
    }

    /// Sets the user-assigned name of the definition.
    /// @param name The name string.
    public void setName(String name) {
        if (name == null) { throw new NullPointerException("Value must not be null"); }
        this.name = name;
    }

    //
    //
    //

    // public int getRed() { return red; }
    // public int getGreen() { return green; }
    // public int getBlue() { return blue; }
    // public int getOpacity() { return opacity; }

    //
    //
    //

    /// Gets the linear interpolation (lerp) value (usually 0.0 to 1.0) used for mixing.
    public String getLerp() {
        return lerp;
    }

    /// Gets the final 6 or 8 digit hex color string.
    public String getHexColor() {
        return hexColor;
    }

    /// Sets the final 6 or 8 digit hex color string.
    /// @param hexColor The hex color string.
    public void setHexColor(String hexColor) {
        if (hexColor == null) { throw new NullPointerException("Value must not be null"); }
        this.hexColor = hexColor;
    }

    /// Gets the hex representation of the left color input for a transform/lerp operation.
    public String getLeftHexColor() {
        return leftHexColor;
    }

    /// Sets the hex representation of the left color input for a transform/lerp operation.
    /// @param hexColor The hex color string.
    public void setLeftHexColor(String hexColor) {
        if (hexColor == null) { throw new NullPointerException("Value must not be null"); }
        this.leftHexColor = hexColor;
    }

    /// Gets the hex representation of the right color input for a transform/lerp operation.
    public String getRightHexColor() {
        return rightHexColor;
    }

    /// Sets the hex representation of the right color input for a transform/lerp operation.
    /// @param hexColor The hex color string.
    public void setRightHexColor(String hexColor) {
        if (hexColor == null) { throw new NullPointerException("Value must not be null"); }
        this.rightHexColor = hexColor;
    }

    /// Sets the linear interpolation (lerp) value.
    /// @param lerp The lerp value string.
    public void setLerp(String lerp) {
        if (lerp == null) { throw new NullPointerException("Value must not be null"); }
        this.lerp = lerp;
    }

    //
    //
    //

    /// Gets the ID of the palette color this definition refers to.
    public String getPaletteColorId() {
        return paletteColorId;
    }

    /// Sets the ID of the palette color this definition refers to.
    /// @param id The palette color ID string.
    public void setPaletteColorId(String id) {
        if (id == null) { throw new NullPointerException("Value must not be null"); }
        this.paletteColorId = id;
    }

    //
    //
    //

    /// Gets the ID of the transform function to be used.
    public String getTransformId() {
        return transformId;
    }

    /// Sets the ID of the transform function to be used.
    /// @param transform The transform ID string.
    public void setTransformId(String transform) {
        if (transform == null) { throw new NullPointerException("Value must not be null"); }
        this.transformId = transform;
    }

    /// Gets the ID of the base color this definition inherits from or uses as input.
    public String getBaseColorId() {
        return baseColorId;
    }

    /// Sets the ID of the base color this definition inherits from or uses as input.
    /// @param id The base color ID string.
    public void setBaseColorId(String id) {
        if (id == null) { throw new NullPointerException("Value must not be null"); }
        this.baseColorId = id;
    }

    /// Gets the string representation of a custom transform function.
    public String getTransformDefinition() {
        return transformDefinition;
    }

    /// Sets the string representation of a custom transform function.
    /// @param transformFunction The transform function string.
    public void setTransformDefinition(String transformFunction) {
        if (transformFunction == null) { throw new NullPointerException("Value must not be null"); }
        this.transformDefinition = transformFunction;
    }

    //
    //
    //

    /// Creates a deep copy of the color definition state, assigning a new unique ID.
    /// @return A new ColorDefinition instance with copied state.
    public ColorDefinition duplicate() {
        ColorDefinition color = new ColorDefinition();
        copyTo(color);
        return color;
    }

    public void copyTo(ColorDefinition to) {
        to.setName(this.name);
        to.setHexColor(this.hexColor);
        to.setLeftHexColor(leftHexColor);
        to.setRightHexColor(rightHexColor);
        to.setLerp(lerp);
        to.setTransformId(transformId);
        to.setTransformDefinition(transformDefinition);
        to.setBaseColorId(baseColorId);
        to.setPaletteColorId(paletteColorId);
    }

    /// Checks if the primary hex color field is blank.
    /// @return true if hexColor is null or contains only whitespace.
    public boolean isBlank() {
        return hexColor.isBlank();
    }

    /// Checks if a transform function ID is defined.
    public boolean usesTransform() {
        return transformId != null && !transformId.isBlank();
    }

    /// Checks if the color is defined by a palette color ID.
    public boolean usesPaletteColor() {
        return paletteColorId != null && !paletteColorId.isBlank();
    }

    /// Checks if a base color ID is defined.
    public boolean usesBaseColor() {
        return baseColorId != null && !baseColorId.isBlank();
    }

    /// Checks if a lerp value is defined, indicating color mixing.
    public boolean usesLerp() {
        return lerp != null && !lerp.isBlank();
    }

    /// Checks if the definition fields that control the color are all empty.
    /// NOTE: This does not check name or ID.
    public boolean isEmpty() {
        return getHexColor().isEmpty() &&
                getTransformDefinition().isEmpty() &&
                getPaletteColorId().isEmpty() &&
                getBaseColorId().isEmpty() &&
                getTransformId().isEmpty() &&
                getLeftHexColor().isEmpty() &&
                getRightHexColor().isEmpty();
    }

    public String toString() {
        return hexColor;
    }
}