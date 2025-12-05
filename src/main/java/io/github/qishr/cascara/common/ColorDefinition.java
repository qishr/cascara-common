package io.github.qishr.cascara.common;

public class ColorDefinition {
    String leftColor = "";
    String rightColor = "";
    String transformDefinition = "";
    String paletteColorBaseId = "";
    String transformId = "";
    String paletteColorId = "";
    String hexColor = "";

    public ColorDefinition() {

    }

    public ColorDefinition(String hexValue) {
        this.hexColor = hexValue;
    }

    public String getLeftColor() {
        return leftColor;
    }

    public void setLeftColor(String baseHexColor) {
        this.leftColor = baseHexColor;
    }

    public String getPaletteColorId() {
        return paletteColorId;
    }

    public void setPaletteColorId(String paletteItem) {
        this.paletteColorId = paletteItem;
    }

    public String getHexColor() {
        return hexColor;
    }

    public void setHexColor(String finalHexColor) {
        this.hexColor = finalHexColor;
    }

    public String getTransformId() {
        return transformId;
    }

    public void setTransformId(String transform) {
        this.transformId = transform;
    }

    public boolean isBlank() {
        return hexColor.isBlank();
    }

    public boolean usesPalette() {
        return !paletteColorId.isBlank();
    }

    public boolean usesTransform() {
        return !transformId.isBlank();
    }

    public String getTransformDefinition() {
        return transformDefinition;
    }

    public void setTransformDefinition(String transformFunction) {
        this.transformDefinition = transformFunction;
    }

    public ColorDefinition duplicate() {
        ColorDefinition color = new ColorDefinition();
        color.setLeftColor(leftColor);
        color.setTransformId(transformId);
        color.setTransformDefinition(transformDefinition);
        color.setPaletteColorId(paletteColorId);
        color.setHexColor(hexColor);
        return color;
    }

    public String getPaletteColorBaseId() {
        return paletteColorBaseId;
    }

    public void setPaletteColorBaseId(String paletteColorDefinition) {
        this.paletteColorBaseId = paletteColorDefinition;
    }

    public String getRightColor() {
        return rightColor;
    }

    public void setRightColor(String rightColor) {
        this.rightColor = rightColor;
    }
}
