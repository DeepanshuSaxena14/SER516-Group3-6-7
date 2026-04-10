import fs from "fs";
import path from "path";
import shell from "shelljs";
import { runPMD } from "../pmdRunner.js";
import { execWithTimeout } from "../utils/utils.js"
import { createDefect, saveDefectCount, getDefectsByRepo, markDefectsFixed } from "../services/mongoApi.js"

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


const cloneRepo = async (githubLink, repoPath) => {
    const result = await execWithTimeout(
      `git clone --depth 1 "${githubLink}" "${repoPath}"`,
      process.env.CLONE_TIMEOUT_MS || 60000
    );
    if (result.code !== 0) {
        throw new Error("Failed to clone repository please make sure the URL is correct and the repo is public");
    }
};

const analyzeRepo = async (repoName, repoPath) => {
    const reportDir = path.resolve("./reports");
    const reportPath = path.join(reportDir, `${repoName}-pmd-report.json`);

    const generatedReportPath = await runPMD(repoPath, reportPath);
    const reportContent = fs.readFileSync(generatedReportPath, "utf-8");
    const reportJson = JSON.parse(reportContent);

    return { reportJson, generatedReportPath };
};

const extractViolations = (reportJson) => {
    return reportJson?.files?.flatMap(file =>
      (file?.violations ?? []).map(v => ({ filepath: file.filename, rule: v.rule, message: v.description }))
    ) ?? [];
};

const defectKey = (d) => `${d.filepath}::${d.rule}::${d.message}`;

const syncDefects = async (repoName, violations) => {
    const existingDefects = await getDefectsByRepo(repoName);

    const existingSet = new Set(existingDefects.map(defectKey));
    const newViolationSet = new Set(violations.map(defectKey));

    const toCreate = violations.filter(v => !existingSet.has(defectKey(v)));
    const toFix = existingDefects
      .filter(d => !newViolationSet.has(defectKey(d)))
      .map(d => d._id);

    const promises = [];
    promises.push(...toCreate.map(v => createDefect(repoName, v.filepath, v.rule, v.message)));

    if (toFix.length > 0) promises.push(markDefectsFixed(toFix));
    
    promises.push(saveDefectCount(repoName, violations.length));
    await Promise.all(promises);
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

    try {
        await cloneRepo(githubLink, repoPath);
        const { reportJson, generatedReportPath } = await analyzeRepo(repoName, repoPath);
        const violations = extractViolations(reportJson);

        await syncDefects(repoName, violations);

        shell.rm("-rf", repoPath);

        return res.status(200).json({
            message: "Repository cloned and PMD analysis complete",
            defectCount: violations.length,
            repoPath,
            reportPath: generatedReportPath,
            report: reportJson
        });
    } catch (error) {
        console.error("Clone and PMD analysis error:", error);
        shell.rm("-rf", repoPath);
        return res.status(500).json({
            message: error.message,
            repoPath,
        });
    }
};