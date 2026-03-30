import { describe, it, expect, beforeEach, afterEach, beforeAll, vi } from "vitest";
import request from "supertest";
import fs from "fs";
import os from "os";
import path from "path";

// We mock shelljs before importing the app
vi.mock("shelljs", async () => {
    const actual = await vi.importActual("shelljs");
    return {
        default: {
            ...actual.default,
            exec: vi.fn(),
        },
    };
});

// We mock runPMD before importing the app
vi.mock("../pmdRunner.js", () => ({
    runPMD: vi.fn((repoPath, reportPath) => {
        fs.mkdirSync(path.dirname(reportPath), { recursive: true });
        fs.writeFileSync(reportPath, JSON.stringify({ ok: true }));
    }),
}));

// The idea is to not actually clone any repos in tests, and run everything in a temp directory
describe("POST /api/github/clone", () => {
    let tmpDir;
    let app;
    let shelljs;
    let originalCwd;

    beforeAll(async () => {
        // We need to import shelljs and app here to ensure the mocks are in place before the controller is loaded
        shelljs = (await import("shelljs")).default;
        app = (await import("../app.js")).default;
    });

    beforeEach(() => {
        originalCwd = process.cwd();
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "ser516-backend-"));
        process.chdir(tmpDir);
        shelljs.exec.mockReset();
    });

    afterEach(async () => {
        process.chdir(originalCwd)
        await new Promise(resolve => setTimeout(resolve, 100));

        try {
            fs.rmSync(tmpDir, { recursive: true, force: true });
        } catch (err) {
            console.warn("Cleanup failed:", err.message);
        }
    });

    it("returns 400 when github_link is missing", async () => {
        const res = await request(app).post("/api/github/clone").send({});
        expect(res.status).toBe(400);
        expect(res.body.message).toMatch(/github_link/i);
    });

    it("returns 200 when clone succeeds and report exists", async () => {
        shelljs.exec.mockReturnValue({ code: 0, stdout: "", stderr: "" });

        const res = await request(app)
            .post("/api/github/clone")
            .send({ github_link: "https://github.com/junit-team/junit-framework.git" });

        expect(res.status).toBe(200);
        expect(res.body.pmd).toEqual({ ok: true });

        expect(shelljs.exec).toHaveBeenCalledTimes(1);
        expect(shelljs.exec.mock.calls[0][0]).toContain("git clone");
    });

    it("returns 400 when clone fails", async () => {
        shelljs.exec.mockReturnValue({ code: 1, stdout: "", stderr: "boom" });

        const res = await request(app)
            .post("/api/github/clone")
            .send({ github_link: "https://github.com/example/repo.git" });

        expect(res.status).toBe(400);
        expect(res.body.message).toMatch(/failed/i);
    });
});