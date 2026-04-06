import { Router } from "express";
import { getLatestDefect } from "../controllers/DefectController.js";

const router = Router();

router.get("/defects/summary", getLatestDefect);

export default router;
