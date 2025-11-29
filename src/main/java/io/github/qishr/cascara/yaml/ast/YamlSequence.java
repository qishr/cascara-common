// YamlSequence.java (Extends YamlNode)
package io.github.qishr.cascara.yaml.ast;

// The items are naturally stored in the inherited 'children' list.
public class YamlSequence extends YamlNode {

    /**
     * Adds an item to the sequence.
     * @param item The YamlNode representing the sequence item's value.
     */
    public void addItem(YamlNode item) {
        super.addChild(item);
    }
}