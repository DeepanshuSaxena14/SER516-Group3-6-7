import { Router } from "express";
import { getLatestDefect, createDefect, saveDefectCount } from "../controllers/DefectController.js";

const router = Router();

router.get("/defects/summary", getLatestDefect);
router.post("/defects", createDefect);
router.post("/defects/count", saveDefectCount);

export default router;