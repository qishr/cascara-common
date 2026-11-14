package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.util.StringUtils.Case;

public class StringUtilsTests {

    //
    // Identification
    //

    @Test
    void test_identifyCamel() {
        assertEquals(Case.CAMEL, Case.of("variableName"));
    }

    @Test
    void test_identifyKebab() {
        assertEquals(Case.KEBAB, Case.of("property-name"));
    }

    @Test
    void test_identifyPascal() {
        assertEquals(Case.PASCAL, Case.of("ClassName"));
    }

    @Test
    void test_identifySnake() {
        assertEquals(Case.SNAKE, Case.of("env_var"));
    }

    //
    // Splitting
    //

    @Test
    void test_splitCamel() {
        String string = "variableName";
        Case stringCase = Case.of(string);
        String[] segments = stringCase.split(string);
        assertEquals(2, segments.length);
        assertEquals("variable", segments[0]);
        assertEquals("name", segments[1]);
    }

    @Test
    void test_splitKebab() {
        String string = "property-name";
        Case stringCase = Case.of(string);
        String[] segments = stringCase.split(string);
        assertEquals(2, segments.length);
        assertEquals("property", segments[0]);
        assertEquals("name", segments[1]);
    }

    @Test
    void test_splitPascal() {
        String string = "ClassName";
        Case stringCase = Case.of(string);
        String[] segments = stringCase.split(string);
        assertEquals(2, segments.length);
        assertEquals("class", segments[0]);
        assertEquals("name", segments[1]);
    }

    @Test
    void test_splitSnake() {
        String string = "env_var";
        Case stringCase = Case.of(string);
        String[] segments = stringCase.split(string);
        assertEquals(2, segments.length);
        assertEquals("env", segments[0]);
        assertEquals("var", segments[1]);
    }

    //
    // Conversions
    //

    @Test
    void test_camelToCamel() {
        assertEquals("variableName", StringUtils.toCamelCase("variableName"));
    }

    @Test
    void test_camelToKebab() {
        assertEquals("variable-name", StringUtils.toKebabCase("variableName"));
    }

    @Test
    void test_camelToPascal() {
        assertEquals("VariableName", StringUtils.toPascalCase("variableName"));
    }

    @Test
    void test_camelToSnake() {
        assertEquals("variable_name", StringUtils.toSnakeCase("variableName"));
    }

    @Test
    void test_dottedToKebab() {
        assertEquals("package-class", StringUtils.toKebabCase("package.class"));
    }

    @Test
    void test_dottedToCamel() {
        assertEquals("packageClass", StringUtils.toCamelCase("package.class"));
    }

    @Test
    void test_dottedToPascal() {
        assertEquals("PackageClass", StringUtils.toPascalCase("package.class"));
    }

    @Test
    void test_dottedToSnake() {
        assertEquals("package_class", StringUtils.toSnakeCase("package.class"));
    }

    @Test
    void test_kebabToCamel() {
        assertEquals("propertyName", StringUtils.toCamelCase("property-name"));
    }

    @Test
    void test_kebabToKebab() {
        assertEquals("property-name", StringUtils.toKebabCase("property-name"));
    }

    @Test
    void test_kebabToPascal() {
        assertEquals("PropertyName", StringUtils.toPascalCase("property-name"));
    }

    @Test
    void test_kebabToSnake() {
        assertEquals("property_name", StringUtils.toSnakeCase("property-name"));
    }

    @Test
    void test_pascalToCamel() {
        assertEquals("className", StringUtils.toCamelCase("ClassName"));
    }

    @Test
    void test_pascalToKebab() {
        assertEquals("class-name", StringUtils.toKebabCase("ClassName"));
    }

    @Test
    void test_pascalToPascal() {
        assertEquals("ClassName", StringUtils.toPascalCase("ClassName"));
    }

    @Test
    void test_pascalToSnake() {
        assertEquals("class_name", StringUtils.toSnakeCase("ClassName"));
    }

    @Test
    void test_snakeToCamel() {
        assertEquals("envVar", StringUtils.toCamelCase("env_var"));
    }

    @Test
    void test_snakeToKebab() {
        assertEquals("env-var", StringUtils.toKebabCase("env_var"));
    }

    @Test
    void test_snakeToPascal() {
        assertEquals("EnvVar", StringUtils.toPascalCase("env_var"));
    }

    @Test
    void test_snakeToSnake() {
        assertEquals("env_var", StringUtils.toSnakeCase("env_var"));
    }
}
