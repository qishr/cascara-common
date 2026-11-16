package io.github.qishr.cascara.test.common.jar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceCodeExtractor {
    private static final Pattern MODULE_PATTERN = Pattern.compile(
        "^\\s*(?:open\\s+)?module\\s+([a-zA-Z0-9_.]+)", Pattern.MULTILINE
    );

    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "^\\s*(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|static\\s+|sealed\\s+|non-sealed\\s+)*?(?:class|enum|record|interface|@interface)\\s+([a-zA-Z0-9_]+)",
        Pattern.MULTILINE
    );

    public static String extractClassName(String sourceCode) {
        // 1. Check for module declaration
        Matcher moduleMatcher = MODULE_PATTERN.matcher(sourceCode);
        if (moduleMatcher.find()) {
            return "module-info";
        }

        // 2. Extract simple type name (class, enum, record, interface)
        Matcher typeMatcher = TYPE_PATTERN.matcher(sourceCode);
        if (typeMatcher.find()) {
            return typeMatcher.group(1);
        }

        return "";
    }
}

// public class SourceCodeExtractor {
//     private static final Pattern PACKAGE_PATTERN = Pattern.compile(
//         "^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE
//     );

//     private static final Pattern TYPE_PATTERN = Pattern.compile(
//         "^\\s*(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|static\\s+|sealed\\s+|non-sealed\\s+)*?(?:class|enum|record|interface|@interface)\\s+([a-zA-Z0-9_]+)",
//         Pattern.MULTILINE
//     );

//     private static final Pattern MODULE_PATTERN = Pattern.compile(
//         "^\\s*(?:open\\s+)?module\\s+([a-zA-Z0-9_.]+)", Pattern.MULTILINE
//     );

//     public static String extractClassName(String sourceCode) {
//         // 1. Check for module declaration
//         Matcher moduleMatcher = MODULE_PATTERN.matcher(sourceCode);
//         if (moduleMatcher.find()) {
//             return "module-info";
//         }

//         // 2. Extract class/enum/record/interface simple name
//         Matcher typeMatcher = TYPE_PATTERN.matcher(sourceCode);
//         if (!typeMatcher.find()) {
//             return "";
//         }
//         String simpleName = typeMatcher.group(1);

//         // 3. Extract package name and combine into FQCN
//         Matcher pkgMatcher = PACKAGE_PATTERN.matcher(sourceCode);
//         if (pkgMatcher.find()) {
//             return pkgMatcher.group(1) + "." + simpleName;
//         }

//         return simpleName;
//     }
// }

// public class SourceCodeExtractor {
//     public static String extractClassName(String sourceCode) {
//         // Two formats expected:
//         // module provider.a {
//         // public class ProviderA implements TestService {

//         String[] lines = sourceCode.split("\n");
//         for (String line : lines) {
//             String trimmed = line.trim();
//             if (trimmed.startsWith("module")) {
//                 return "module-info";
//             } else if (trimmed.contains("class ")) {
//                 int classPos = trimmed.indexOf("class ");
//                 int classNamePos = classPos + 6;
//                 String classNameEtc = trimmed.substring(classNamePos);
//                 int space = classNameEtc.indexOf(" ");
//                 String className = classNameEtc.substring(0, space);
//                 return className;
//             }
//         }

//         return "";
//     }
// }
