import fs from 'fs';
import path from 'path';
import shell from 'shelljs';
import { runPMD } from '../pmdRunner.js';


export const cloneRepo = (req, res) => {
    const githubLink = req.body.github_link;

    // Using a single stable workspace volume inside the container for all repo and PMD related actions
    const repoPath = path.resolve("./work/target-repo");
    const reportPath = path.resolve("./work/reports/pmd-report.json");

    shell.rm("-rf", repoPath);
    shell.mkdir("-p", path.dirname(reportPath));

    // Validate input
    if (!githubLink) {
      return res.status(400).json({ message: "Missing github_link in request body" });
    }

    const gitCloneResult = shell.exec(`git clone "${githubLink}" "${repoPath}"`, { silent: true });
    if (gitCloneResult.code !== 0) {
      return res.status(400).json({
        message: 'Failed to clone repository please make sure the URL is correct and the repo is public',
        stderr: gitCloneResult.stderr
      });
    }

    try {
      runPMD(repoPath, reportPath);
      if (!fs.existsSync(reportPath)) {
        return res.status(500).json({
          message: "PMD analysis failed (no report generated).",
        });
      }
      const json = JSON.parse(fs.readFileSync(reportPath, "utf-8"));
      
      return res.status(200).json({ message: 'Repository cloned and analyzed successfully', pmd: json, });

    } catch (error) {
      console.error("Error during PMD analysis: ", error);
      return res.status(500).json({ message: 'PMD analysis failed', error: error.message });
    }

};
