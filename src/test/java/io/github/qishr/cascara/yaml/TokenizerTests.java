package io.github.qishr.cascara.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.yaml.ast.YamlNode;
import io.github.qishr.cascara.yaml.parser.Tokenizer;
import io.github.qishr.cascara.yaml.parser.Tokenizer.Token;

class TokenizerTests {


    @Test
    void test1() {
        String yamlString = "device:\n  martin-mia1: scalar";
        List<Token> tokens = Tokenizer.tokenize(yamlString, true);
        assertNotNull(tokens);
    }

    @Test
    void test2() {
        String yamlString = "device:\n  martin-mia1:\n" +
                            "    host: martin-mia1\n" +
                            "    path: /sdcard/\n" +
                            "    user: mu\n";
        List<Token> tokens = Tokenizer.tokenize(yamlString, true);
        assertNotNull(tokens);
    }
}
