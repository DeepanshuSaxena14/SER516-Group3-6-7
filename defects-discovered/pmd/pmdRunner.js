import fs from 'fs';
import shell from "shelljs";
import path from "path";

const PMD_TIMEOUT_MS = 120000; //120 seconds

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

export const runPMD = async (repoPath, reportPath) => {
    shell.mkdir("-p", path.dirname(reportPath));

    console.log("Running PMD analysis...");

    // Running PMD CLI directly since PMD's official Docker image does not support 
    // outputting to a file when running from another container
    const command =
        `pmd check -d "${repoPath}" ` +
        `-R "${path.resolve("./pmd/ruleset.xml")}" ` +
        `-f json -r "${reportPath}" ` +
        `--no-fail-on-error --no-cache`;

    const result = await execWithTimeout(command, PMD_TIMEOUT_MS);
    console.log("PMD analysis complete.");

    if (result.code !== 0 && result.code !== 4) {
        console.error("PMD stdout:\n", result.stdout);
        console.error("PMD stderr:\n", result.stderr);
        throw new Error(result.stderr || result.stdout || "PMD failed !");
    }

    if (!fs.existsSync(reportPath)) {
        throw new Error("PMD report was not generated.");
    }

    return reportPath;

};
