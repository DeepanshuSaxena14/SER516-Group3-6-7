import { Router } from "express";
import {
  createFocusFactor,
  getStats,
  updateStat,
  deleteStatById,
} from "../controllers/mongoController.js";

const router = Router();

router.post("/stats", createFocusFactor);
router.get("/stats", getStats);
router.put("/stats/:id", updateStat);
router.delete("/stats/:id", deleteStatById);

export default router;
