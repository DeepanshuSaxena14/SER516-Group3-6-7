package org.github;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;


public class CloneObject {

    //object to get and clone the repo.

String repoUrl;
static String branchName;

    public CloneObject(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public static String cloneRepository(String repoUrl) throws Exception {

        Path path = org.service.StorageConfig.getClonePath(); // changed to improve storage
        File localPath = path.toFile();

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
            localPath.mkdirs();
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPath)
                .setBranch(branchName != null ? branchName : "main")
                .call()
                .close();

        System.out.println("Repo cloned.");

        return localPath.getAbsolutePath();
    }

    public static void getRepoMetadata(String repoUrl) throws Exception {

    Path path = org.service.StorageConfig.getTempPath(); // configurable storage path for temp repo
    File localPath = path.toFile();
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        localPath.mkdirs();
        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPath)
                .call();

        Repository repository = git.getRepository();
        String branch;
        if (repository.findRef("main") != null) {
            branch = "main";
            branchName = "main";
        } else {
            branch = "master";
            branchName = "master";
        }
        git.checkout().setName(branch).call();
        Iterable<RevCommit> commits = git.log().setMaxCount(1).call();

        for (RevCommit commit : commits) {
            System.out.println("Latest Commit:");
            System.out.println("Message: " + commit.getFullMessage());
            System.out.println("Author: " + commit.getAuthorIdent().getName());
            System.out.println("Commit ID: " + commit.getName());
        }

        git.close();
    }

    public String getRepoUrl() {
        return repoUrl;
    }
}

