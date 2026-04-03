import shell from "shelljs";

export const cloneRepo = (req, res) => {

    if (!req.body.github_link) {
        return res
            .status(400)
            .json({ message: "Missing required field: github_link" });
    }
    const githubLink = req.body.github_link;
    const repoName = githubLink.split("/").pop();

    //Need to change it later to add the repo to the volume of the container
    const repoPath = `./repos/${repoName}`;

    if (shell.exec(`git clone ${githubLink} ${repoPath}`).code !== 0) {
        return res
            .status(500)
            .json({ message: "Failed to clone repository" });
    } else {
        return res.status(200).json({
            message: "Repository cloned successfully",
            repoPath,
        });
    }
};

