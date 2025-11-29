package io.github.qishr.cascara.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.yaml.Yaml;
import io.github.qishr.cascara.yaml.ast.YamlMap;
import io.github.qishr.cascara.yaml.ast.YamlMappingEntry;
import io.github.qishr.cascara.yaml.ast.YamlScalar;
import io.github.qishr.cascara.yaml.ast.YamlScalar.ScalarStyle;

public class CascProject {
    protected Reporter reporter = null;

    protected Path projectDirectory = null;
    protected Path projectFile = null;
    protected InputStream inputStream = null;
    protected String name = "";

    private Yaml projectYaml = null;
    private String builderExtensionName = null;
    private String projectExtensionName = null;

    protected CascProject(Reporter reporter, Path projectFile) {
        this.reporter = reporter;
        this.projectFile = projectFile;
        if (projectFile != null) {
            this.projectDirectory = projectFile.getParent();
        }
    }

    protected CascProject(Reporter reporter, InputStream inputStream) {
        this.reporter = reporter;
        this.inputStream = inputStream;
    }

    public static CascProject create(Reporter reporter, Path projectFile) throws IOException {
        CascProject project = new CascProject(reporter, projectFile);
        // TODO: Write the YAML file
        return project;
    }

    public static CascProject open(Reporter reporter, Path projectFile) throws IOException {
        CascProject project = new CascProject(reporter, projectFile);
        project.load();
        return project;
    }

    protected void load() throws IOException {
        if (projectFile != null) {
            this.projectYaml = Yaml.readFile(projectFile, StandardCharsets.UTF_8);
        } else if (inputStream != null) {
            this.projectYaml = Yaml.load(inputStream);
        } else {
            throw new IOException("No input");
        }
        this.builderExtensionName = projectYaml.getString("builderExtension");
        this.projectExtensionName = projectYaml.getString("projectExtension");
        this.name = projectYaml.getString("name");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getProjectDirectory() {
        return projectDirectory;
    }

    public void setProjectDirectory(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    public Path getProjectFile() {
        return projectFile;
    }

    public void setProjectFile(Path projectFile) {
        this.projectFile = projectFile;
    }

    public Yaml getProjectYaml() {
        return projectYaml;
    }

    public void setProjectYaml(Yaml projectYaml) {
        this.projectYaml = projectYaml;
    }

    public String getBuilderExtensionName() {
        return builderExtensionName;
    }

    public void setBuilderExtensionName(String builderExtensionName) {
        this.builderExtensionName = builderExtensionName;
    }

    public String getProjectExtensionName() {
        return projectExtensionName;
    }

    public void setProjectExtensionName(String projectExtensionName) {
        this.projectExtensionName = projectExtensionName;
    }

    public Yaml getYaml() {
        YamlMap root = new YamlMap();
        if (builderExtensionName != null && !builderExtensionName.isBlank()) {
            addStringEntry(root, "builderExtension", builderExtensionName, ScalarStyle.PLAIN);
        }
        if (projectExtensionName != null && !projectExtensionName.isBlank()) {
            addStringEntry(root, "projectExtension", projectExtensionName, ScalarStyle.PLAIN);
        }
        addStringEntry(root, "name", name, ScalarStyle.DOUBLE_QUOTED);
        return new Yaml(root);
    }

    protected void addStringEntry(YamlMap root, String k, String v, ScalarStyle style) {
        YamlScalar k1 = new YamlScalar(k, ScalarStyle.PLAIN);
        YamlScalar v1 = new YamlScalar(v, style);
        root.addEntry(new YamlMappingEntry(k1, v1));
    }
}
