package org.service;

import java.nio.file.Path;
import java.util.Set;

public record FileData(
        Path filePath,
        String className,
        Set<String> imports
) {

    //empty body - don't touch me please.
}
