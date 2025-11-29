package io.github.qishr.cascara.yaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.yaml.ast.YamlMap;
import io.github.qishr.cascara.yaml.ast.YamlNode;
import io.github.qishr.cascara.yaml.parser.Parser;
import io.github.qishr.cascara.yaml.parser.Tokenizer;
import io.github.qishr.cascara.yaml.parser.YamlEmitter;
import io.github.qishr.cascara.yaml.parser.Tokenizer.Token;

/**
 * Represents a complete YAML document, handling loading, saving, and providing
 * entry to the AST traversal methods exposed by the root YamlNode.
 */
public class Yaml {
    private YamlNode root;

    // --- Static Loading Methods ---

    public static Yaml readFile(Path path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(path);
        String yamlString = new String(encoded, encoding);
        return new Yaml(yamlString);
    }

    public static Yaml load(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String yamlString = br.lines().collect(Collectors.joining("\n"));
            return new Yaml(yamlString);
        }
    }

    // --- Constructors ---

    public Yaml(String yamlString) {
        // NOTE: Keeping verbose flag set to true, adjust as needed.
        List<Token> tokens = Tokenizer.tokenize(yamlString, true);
        Reporter reporter = new Reporter();
        reporter.setLevel(Diagnostic.Level.INFO);
        Parser parser = new Parser(reporter, tokens);
        this.root = parser.parse();
    }

    public Yaml(YamlNode root) {
        this.root = root;
    }

    private Yaml() {
        // Hide this constructor
    }

    // --- Getter ---

    public YamlNode getRoot() {
        return root;
    }

    // --- New Saving/Serialization Methods ---

    /**
     * Saves the YAML document to a file using the specified path and encoding.
     * @param path The path to the file to write.
     * @param encoding The character encoding to use.
     * @throws IOException If an I/O error occurs.
     */
    public void saveToFile(Path path, Charset encoding) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, encoding)) {
            new YamlEmitter().emit(root, writer);
        }
    }

    /**
     * Writes the YAML document content to an OutputStream.
     * @param os The output stream to write to.
     * @throws IOException If an I/O error occurs.
     */
    public void writeToStream(OutputStream os) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
            new YamlEmitter().emit(root, writer);
        }
    }

    /**
     * Serializes the YAML document to a String.
     * @return The string representation of the YAML document.
     */
    public String dump() {
        // Use a fast, non-IO-dependent Emitter method if available, or simulate it.
        // For now, we assume YamlEmitter can return a String.
        return new YamlEmitter().emitToString(root);
    }

    // --- Cleaned-up Traversal Methods (Delegate to root) ---
    // These methods now just act as convenience wrappers for the root node.

    public YamlNode findNode(String... path) {
        return root.findNode(path);
    }

    public String getString(String... path) {
        return root.getString(path);
    }

    public List<YamlNode> getSequence(String... path) {
        return root.getSequence(path);
    }

    public YamlMap getMap(String ... path) {
        return root.getMap(path);
    }
}
