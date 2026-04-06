import fs from "fs";
import path from "path";
import shell from "shelljs";
import { execWithTimeout } from "../utils/utils.js";
import { runPMD } from "../pmdRunner.js";

const CLONE_TIMEOUT_MS = parseInt(process.env.CLONE_TIMEOUT_MS) || 60000;

export const cloneRepo = async (req, res) => {

  if (!req.body.github_link) {
    return res
      .status(400)
      .json({ message: "Missing required field: github_link" });
  }
  const githubLink = req.body.github_link;
  const repoName = githubLink.split("/").pop();

  //Need to change it later to add the repo to the volume of the container
  const repoPath = `./repos/${repoName}`;

  shell.rm("-rf", repoPath);
  const reportPath = `./repos/${repoName}-report.json`;
  shell.mkdir("-p", `./repos`);

  try {
    const gitCloneResult = await execWithTimeout(
      `git clone --depth 1 "${githubLink}" "${repoPath}"`,
      CLONE_TIMEOUT_MS
    );

    if (gitCloneResult.code !== 0) {
      return res.status(400).json({
        message: 'Failed to clone repository please make sure the URL is correct and the repo is public',
        stderr: gitCloneResult.stderr
      });
    }
    await runPMD(repoPath, reportPath);
    if (!fs.existsSync(reportPath)) {
      return res.status(500).json({
        message: "PMD analysis failed (no report generated).",
      });
    }
    const json = JSON.parse(fs.readFileSync(reportPath, "utf-8"));
    const defectCount = (json.violations || []).length;

    return res.status(200).json({
      message: 'Repository cloned and analyzed successfully',
      pmd: json,
      defectCount
    });

  } catch (error) {
    console.error("Error during clone or PMD analysis: ", error);
    if (error.message.includes('timed out')) {
      return res.status(504).json({ message: error.message });
    }
    return res.status(500).json({ message: "Internal server error", error: error.message });
  }
}
