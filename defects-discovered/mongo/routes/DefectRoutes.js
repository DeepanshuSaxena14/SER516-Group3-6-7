import { Router } from "express";
import { getLatestDefect, createDefect, saveDefectCount, getLatestDefectCount } from "../controllers/DefectController.js";

const router = Router();

router.get("/defects/summary", getLatestDefect);
router.post("/defects", createDefect);
router.post("/defects/count", saveDefectCount);
router.get("/defects/count/:repoName", getLatestDefectCount);

export default router;