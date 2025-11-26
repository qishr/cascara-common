package io.github.qishr.cascara.yaml.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.qishr.cascara.yaml.parser.Tokenizer.Token;

public class YamlNode {
    YamlNode parent = null;
    List<YamlNode> children = new ArrayList<>();
    String anchorName = null;
    String tagName = null;
    Token startToken;

    // --- Structural AST Methods ---

    public void addChild(YamlNode child) {
        if (child != null) {
            child.setParent(this);
            this.children.add(child);
        }
    }

    public void setParent(YamlNode parent) {
        this.parent = parent;
    }

    public YamlNode getParent() {
        return parent;
    }

    public List<YamlNode> getChildren() {
        return children;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Token getStartToken() {
        return startToken;
    }

    public void setStartToken(Token startToken) {
        this.startToken = startToken;
    }

    // -------------------------------------------------------------------------
    // Path Traversal and Utility Methods (Cleanup Applied)
    // -------------------------------------------------------------------------

    /**
     * Attempts to find a YamlNode at a specific path (sequence of map keys).
     *
     * @param path The sequence of map keys to follow, starting from the current node (`this`).
     * @return The YamlNode at the path, or null if not found.
     */
    public YamlNode findNode(String... path) {
        YamlNode currentNode = this;

        for (String key : path) {
            if (currentNode == null || !(currentNode instanceof YamlMap)) {
                // Cannot traverse further if the current node is null or not a map
                return null;
            }

            YamlMap map = (YamlMap) currentNode;
            // Delegate the key lookup to the YamlMap class's robust method.
            currentNode = map.getValueByKey(key);
        }

        return currentNode;
    }

    /**
     * Extracts a scalar value at the given path and converts it to a String.
     *
     * @param path The path to the scalar node.
     * @return The String value, or null if the path is invalid or the node is not a scalar.
     */
    public String getString(String... path) {
        YamlNode node = findNode(path);
        if (node instanceof YamlScalar yamlScalar) {
            // Assuming YamlScalar.asString() is the preferred way to get the string value
            return yamlScalar.asString();
        }
        return null;
    }

    /**
     * Extracts a list of nodes (items) from a YamlSequence at the given path.
     *
     * @param path The path to the sequence node.
     * @return A List of YamlNode items, or an empty list if the path is invalid or the node is not a sequence.
     */
    public List<YamlNode> getSequence(String... path) {
        YamlNode node = findNode(path);
        if (node instanceof YamlSequence yamlSequence) {
            // Assuming YamlSequence's content is exposed via getChildren()
            return yamlSequence.getChildren();
        }
        return Collections.emptyList();
    }

    /**
     * Extracts a YamlMap at the given path.
     * * @param path The path to the map node.
     * @return The YamlMap, or a new, empty YamlMap if the path is invalid or the node is not a map.
     */
    public YamlMap getMap(String ... path) {
        YamlNode node = findNode(path);
        if (node instanceof YamlMap yamlMap) {
            return yamlMap;
        }
        // Return an empty map instead of null to simplify caller logic (Null Object Pattern).
        return new YamlMap();
    }
}