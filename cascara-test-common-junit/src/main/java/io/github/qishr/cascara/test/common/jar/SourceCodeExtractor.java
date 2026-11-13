package io.github.qishr.cascara.test.common.jar;

public class SourceCodeExtractor {
    public static String extractClassName(String sourceCode) {
        // Two formats expected:
        // module provider.a {
        // public class ProviderA implements TestService {

        String[] lines = sourceCode.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("module")) {
                return "module-info";
            } else if (trimmed.contains("class ")) {
                int classPos = trimmed.indexOf("class ");
                int classNamePos = classPos + 6;
                String classNameEtc = trimmed.substring(classNamePos);
                int space = classNameEtc.indexOf(" ");
                String className = classNameEtc.substring(0, space);
                return className;
            }
        }

        return "";
    }
}
