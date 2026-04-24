package io.github.qishr.cascara.common.content;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.content.ContentType;
import io.github.qishr.cascara.common.lang.annotation.DataField;

public class ContentType {
    @DataField
    /// Unique ID, used for menu items etc
    private String id = "";

    @DataField
    protected String name = "";

    @DataField
    protected List<String> mimeTypes = new ArrayList<>();

    @DataField
    protected List<String> suffixes = new ArrayList<>();

    public ContentType() {}

    public ContentType(String name) {
        this.name = name;
    }

    public ContentType withMimeType(String mimeType) {
        mimeTypes.add(mimeType);
        return this;
    }

    public ContentType withSuffix(String suffix) {
        suffixes.add(suffix);
        return this;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String canonicalName) {
        this.name = canonicalName;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public List<String> getSuffixes() {
        return suffixes;
    }

    public void setSuffixes(List<String> suffixes) {
        this.suffixes = suffixes;
    }

    //
    //
    //

    public boolean matches(String mimeType) {
        if (mimeTypes.contains(mimeType)) {
            return true;
        }
        for (String localType : mimeTypes) {
            if (localType.endsWith("/*")) {
                int slash = localType.indexOf("/");
                String baseType = localType.substring(0, slash);
                if (mimeType.startsWith(baseType + "/")) {
                    return true;
                }
            }
            if (mimeType.endsWith("/*")) {
                int slash = mimeType.indexOf("/");
                String baseType = mimeType.substring(0, slash);
                if (localType.startsWith(baseType + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matches(ContentType contentType) {
        if (contentType == null) {
            return false;
        }
        for (String metaMimeType : contentType.getMimeTypes()) {
            if (matches(metaMimeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (mimeTypes.isEmpty()) {
            return id;
        }
        return mimeTypes.getFirst();
    }
}
