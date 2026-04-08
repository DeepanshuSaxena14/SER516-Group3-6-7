import { Router } from "express";
import { getLatestDefect, createDefect } from "../controllers/DefectController.js";

const router = Router();

router.get("/defects/summary", getLatestDefect);
router.post("/defects", createDefect);

export default router;