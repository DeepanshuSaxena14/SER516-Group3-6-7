import fs from 'fs';
import path from 'path';
import shell from 'shelljs';
import { runPMD } from '../pmdRunner.js';

const CLONE_TIMEOUT_MS = 60000; //60 seconds

const execWithTimeout = (command, timeoutMs) => {
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
            reject(new Error(`Command timed out after ${timeoutMs / 1000}s: ${command}`));
        }, timeoutMs);

        shell.exec(command, { silent: true, async: true }, (code, stdout, stderr) => {
            clearTimeout(timer);
            resolve({ code, stdout, stderr });
        });
    });
};

export const cloneRepo = async (req, res) => {
    const githubLink = req.body.github_link;

    // Validate input
    if (!githubLink) {
      return res.status(400).json({ message: "Missing github_link in request body" });
    }
    
    // Using a single stable workspace volume inside the container for all repo and PMD related actions
    const repoPath = path.resolve("./work/target-repo");
    const reportPath = path.resolve("./work/reports/pmd-report.json");

    shell.rm("-rf", repoPath);
    shell.mkdir("-p", path.dirname(reportPath));

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
      
      return res.status(200).json({ message: 'Repository cloned and analyzed successfully', pmd: json, });

    } catch (error) {
    console.error("Error during clone or PMD analysis: ", error);
    if (error.message.includes('timed out')) {
        return res.status(504).json({ message: error.message });
    }
    return res.status(500).json({ message: 'PMD analysis failed', error: error.message });
    }

};
