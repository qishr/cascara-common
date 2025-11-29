package io.github.qishr.cascara.yaml.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

import io.github.qishr.cascara.yaml.ast.YamlMap;
import io.github.qishr.cascara.yaml.ast.YamlMappingEntry;
import io.github.qishr.cascara.yaml.ast.YamlNode;
import io.github.qishr.cascara.yaml.ast.YamlScalar;
import io.github.qishr.cascara.yaml.ast.YamlSequence;

/**
 * Converts a YamlNode AST back into a YAML document string/stream.
 */
public class YamlEmitter {

    private static final String INDENT_STEP = "  ";

    /**
     * Serializes the YAML AST rooted at the given node and writes it to the writer.
     * @param root The root node of the AST.
     * @param writer The buffer writer to output the YAML text.
     * @throws IOException If a writing error occurs.
     */
    public void emit(YamlNode root, BufferedWriter writer) throws IOException {
        if (root == null) {
            return;
        }
        // Start emission at level 0
        emitNode(root, writer, 0, false);
        writer.flush();
    }

    /**
     * Serializes the YAML AST rooted at the given node and returns it as a String.
     * @param root The root node of the AST.
     * @return The string representation of the YAML document.
     */
    public String emitToString(YamlNode root) {
        if (root == null) {
            return "";
        }
        try (StringWriter sw = new StringWriter();
             BufferedWriter writer = new BufferedWriter(sw)) {
            emit(root, writer);
            return sw.toString();
        } catch (IOException e) {
            // Should not happen with StringWriter/BufferedWriter
            return "ERROR: Failed to serialize AST to string: " + e.getMessage();
        }
    }

    // --- Core Recursive Logic ---

    /**
     * Emits a single YamlNode and its children recursively.
     * @param node The node to emit.
     * @param writer The output writer.
     * @param indentLevel The current indentation level (0 for the root).
     * @param isSequenceItem True if this node is being emitted as an item in a sequence (affects inline vs. block style).
     * @throws IOException If a writing error occurs.
     */
    // In YamlEmitter.java
    // Method: private void emitNode(...)
    // In YamlEmitter.java
    // Method: private void emitNode(...)

    private void emitNode(YamlNode node, BufferedWriter writer, int indentLevel, boolean isSequenceItem) throws IOException {
        if (node == null) {
            return;
        }

        // 1. Handle YamlScalar
        if (node instanceof YamlScalar scalar) {
            writer.write(formatScalar(scalar));

        // 2. Handle YamlMap
        } else if (node instanceof YamlMap map) {

            // No special check for isSequenceItem needed here.
            // The indentLevel passed in *is* the correct indent for the map keys.
            int keyIndent = indentLevel;

            for (YamlMappingEntry entry : map.getEntries()) {
                YamlNode keyNode = entry.getKey();
                YamlNode valueNode = entry.getValue();

                // Write indentation for the key
                writeIndentation(writer, keyIndent);

                // Write Key
                writer.write(formatScalar((YamlScalar) keyNode));
                writer.write(": ");

                // Check Value Type
                boolean valueIsBlock = valueNode instanceof YamlMap || valueNode instanceof YamlSequence;

                if (valueIsBlock) {
                    // Block value (nested map or sequence) starts on the next line
                    writer.newLine();

                    // Recursively emit the block. The block's content must be indented one level deeper.
                    // NOTE: We pass keyIndent + 1 as the new base.
                    emitNode(valueNode, writer, keyIndent + 1, false);
                } else {
                    // Scalar value is written inline
                    emitNode(valueNode, writer, indentLevel, false);

                    // Only add newline here for inline scalars, as block structures handle their own final newline.
                    writer.newLine();
                }
            }
        // 3. Handle YamlSequence
        } else if (node instanceof YamlSequence sequence) {

            // The sequence block should not insert a newline if it's the root or if the parent map already did.
            // The most reliable approach is for the Map block to call writeIndentation and newLine,
            // but since we simplified Map, the Sequence must handle the indent for the dash.

            // NOTE: The map block (case 2) already inserted a newline before calling us if we are a block value.

            for (YamlNode item : sequence.getChildren()) {

                // 1. Write indentation for the dash. This is the indentLevel passed in.
                writeIndentation(writer, indentLevel);
                writer.write("- ");

                // 2. Check item type
                boolean itemIsBlock = item instanceof YamlMap || item instanceof YamlSequence;

                if (itemIsBlock) {
                    // A block item must start its content on a newline, indented one step past the dash.
                    writer.newLine();

                    // Recursively emit the block item's content.
                    // The new indent level is increased by 1 (one indent step past the dash).
                    // Set isSequenceItem=false for the block's content itself.
                    emitNode(item, writer, indentLevel + 1, false);
                } else {
                    // Scalar item is written inline after the "- "
                    emitNode(item, writer, indentLevel, false);
                    writer.newLine();
                }
            }
        }
    }

    // --- Utility Methods ---

    private String formatScalar(YamlScalar scalar) {
        Object value = scalar.getValue();
        if (value == null) {
            return "null";
        }
        String strValue = value.toString();

        // --- ADDED: Respect the ScalarStyle for quoting ---
        if (scalar.getStyle() == YamlScalar.ScalarStyle.DOUBLE_QUOTED) {
            return "\"" + strValue + "\"";
        }
        // Add logic for SINGLE_QUOTED, etc., as needed.
        // PLAIN and other styles return the raw string.

        return strValue;
    }

    private void writeIndentation(BufferedWriter writer, int indentLevel) throws IOException {
        for (int i = 0; i < indentLevel; i++) {
            writer.write(INDENT_STEP);
        }
    }
}