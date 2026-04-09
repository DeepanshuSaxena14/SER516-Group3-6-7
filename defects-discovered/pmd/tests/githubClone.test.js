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
        return reportPath;
    }),
}));

vi.mock("../services/mongoApi.js", () => ({
    getDefectsByRepo: vi.fn().mockResolvedValue([]),
    createDefect: vi.fn().mockResolvedValue({}),
    markDefectsFixed: vi.fn().mockResolvedValue({}),
    saveDefectCount: vi.fn().mockResolvedValue({}),
}));

// The idea is to not actually clone any repos in tests, and run everything in a temp directory
describe("GET /api/pmd/analyze", () => {
    let tmpDir;
    let app;
    let shelljs;
    let originalCwd;

    beforeAll(async () => {
        // We need to import shelljs and app here to ensure the mocks are in place before the controller is loaded
        shelljs = (await import("shelljs")).default;
        app = (await import("../server.js")).default;
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
        const res = await request(app).get("/api/pmd/analyze")
        expect(res.status).toBe(400);
    });

    it("returns 200 when clone succeeds", async () => {
        shelljs.exec.mockImplementation((cmd, opts, callback) => {
            callback(0, "", "");
        });

        const res = await request(app)
            .get("/api/pmd/analyze?github_link=https://github.com/kgary/ser516public.git")

        expect(res.status).toBe(200);

        expect(shelljs.exec).toHaveBeenCalledTimes(1);
        expect(shelljs.exec.mock.calls[0][0]).toContain("git clone");
    });
});