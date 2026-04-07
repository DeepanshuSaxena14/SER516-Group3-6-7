import fs from "fs";
import path from "path";
import shell from "shelljs";
import { runPMD } from "../pmdRunner.js";
import { execWithTimeout } from "../utils/utils.js"

export const runPMDAnalysis = async (req, res) => {
    const { repoPath } = req.body;

    if (!repoPath) {
        return res.status(400).json({ message: "repoPath is required" });
    }

    try {
        const repoName = path.basename(repoPath);
        const reportDir = path.resolve("./reports");
        const reportPath = path.join(reportDir, `${repoName}-pmd-report.json`);

        const generatedReportPath = await runPMD(repoPath, reportPath);
        const reportContent = fs.readFileSync(generatedReportPath, "utf-8");
        const reportJson = JSON.parse(reportContent);

        return res.status(200).json({
            message: "PMD analysis complete",
            reportPath: generatedReportPath,
            report: reportJson,
        });
    } catch (error) {
        console.error("PMD analysis error:", error);
        return res.status(500).json({
            message: "Failed to run PMD analysis",
            error: error.message,
        });
    }
};


export const cloneAndAnalyzeRepo = async (req, res) => {
    const githubLink = req.query.github_link;

    if (!githubLink) {
        return res
            .status(400)
            .json({ message: "github_link query parameter is required" });
    }

    const repoName = githubLink.split("/").pop();
    const repoPath = `./repos/${repoName}`;

    const gitCloneResult = await execWithTimeout(
      `git clone --depth 1 "${githubLink}" "${repoPath}"`,
      process.env.CLONE_TIMEOUT_MS || 60000)

    if (gitCloneResult.code !== 0) {
        return res
            .status(500)
            .json({ message: "Failed to clone repository please make sure the URL is correct and the repo is public" });
    }

    try {
    const reportDir = path.resolve("./reports");
        const reportPath = path.join(
            reportDir,
            `${repoName}-pmd-report.json`,
        );

        const generatedReportPath = await runPMD(repoPath, reportPath);
        const reportContent = fs.readFileSync(generatedReportPath, "utf-8");
        const reportJson = JSON.parse(reportContent);

        const defectCount = reportJson?.files?.reduce((total, file) => {
          return total + (file?.violations?.length ?? 0)
        }, 0)

        shell.rm("-rf", repoPath);

        return res.status(200).json({
            message: "Repository cloned and PMD analysis complete",
            defectCount,
            repoPath,
            reportPath: generatedReportPath,
            report: reportJson
        });

      } catch (error) {
          console.error("Clone and PMD analysis error:", error);
          return res.status(500).json({
            message: "Failed to run PMD analysis after cloning repository",
            error: error.message,
            repoPath,
        });
    }
};