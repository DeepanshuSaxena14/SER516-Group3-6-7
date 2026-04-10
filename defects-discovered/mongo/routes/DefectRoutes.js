import { Router } from "express";
import { createDefect, saveDefectCount, getLatestDefectCount, getLatestDefectDetails, markDefectsFixed, getLatestRepoSummary, getLatestRepoDefectCounts, getLatestRepoDefectDetails } from "../controllers/DefectController.js";

const router = Router();

router.post("/defects", createDefect);
router.post("/defects/count", saveDefectCount);
router.get("/defects/count/:repoName", getLatestDefectCount);
router.get("/defects/latest/:repoName", getLatestDefectDetails);
router.get("/defects/latest", getLatestRepoDefectDetails);
router.patch("/defects/fix", markDefectsFixed);
router.get("/defects/summary/latest", getLatestRepoSummary);
router.get("/defects/count/history/latest", getLatestRepoDefectCounts);

export default router;