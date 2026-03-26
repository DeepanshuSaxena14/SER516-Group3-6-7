package org.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public class Parser {

    public static FileData parse(Path file) throws IOException {

        String content = Files.readString(file);
        String fullname;

        String packageName = "";
        String className = "";
        Set<String> imports = new HashSet<>();

        Pattern packagePattern =
                Pattern.compile("package\\s+([\\w\\.]+);");
        Pattern classPattern =
                Pattern.compile("class\\s+(\\w+)");


        Pattern importPattern =
                Pattern.compile("import\\s+([\\w\\.]+);");


        Matcher matcher = packagePattern.matcher(content);
        if (matcher.find()) {
            packageName = matcher.group(1);
        }
        Matcher cmatcher = classPattern.matcher(content);
        if (cmatcher.find()) {
            className = cmatcher.group(1);
        }
        Matcher imatcher = importPattern.matcher(content);
        while (imatcher.find()) {
            imports.add(imatcher.group(1));
        }



        if (packageName.isEmpty()) {
            fullname = className;
        } else {
            fullname = packageName + "." + className;
        }

        return new FileData(
                file,
                fullname,
                imports
        );
    }
}
