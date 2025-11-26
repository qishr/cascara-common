package io.github.qishr.cascara.yaml.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlDocument {
    String versionDirective = null;
    Map<String, String> tagDirectives = new HashMap<>();
    List<YamlNode> children = new ArrayList<>();

    public String getVersionDirective() {
        return versionDirective;
    }

    public void setVersionDirective(String versionDirective) {
        this.versionDirective = versionDirective;
    }

    public Map<String, String> getTagDirectives() {
        return tagDirectives;
    }

    public void setTagDirectives(Map<String, String> tagDirectives) {
        this.tagDirectives = tagDirectives;
    }

    public YamlNode getContent() {
        return children.isEmpty() ? null : children.get(0);
    }

    public void addContent(YamlNode item) {
        children.add(item);
    }
}
