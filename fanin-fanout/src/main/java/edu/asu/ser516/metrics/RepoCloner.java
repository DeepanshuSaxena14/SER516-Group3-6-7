package edu.asu.ser516.metrics;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class RepoCloner {

    private static final String CLONE_DIR = "/tmp/g6-cloned-repo";

    private RepoCloner() {}

    /**
     * Clones the given GitHub repo URL to a fixed local path.
     * Deletes any previously cloned repo first.
     *
     * @param repoUrl public GitHub HTTPS URL
     * @return Path to the root of the cloned repository
     */
    public static Path cloneRepo(String repoUrl) throws Exception {
        Path clonePath = Path.of(CLONE_DIR);

        // Clean up previous clone
        if (Files.exists(clonePath)) {
            Files.walk(clonePath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(clonePath);

        System.out.println("Cloning repository: " + repoUrl);

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(clonePath.toFile())
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider("", "")
                )
                .call()
                .close();

        System.out.println("Repository cloned successfully to: " + clonePath);
        return clonePath;
    }
}