import { Router } from "express"
import { createFocusFactor, getStats, updateStat, deletStatbyId } from "../controllers/CrudController.js";

const router = Router()

router.post("/stats", createFocusFactor);
router.get("/stats", getStats);
router.put("/stats/:id", updateStat);
router.delete("/stats/:id", deletStatbyId);

export default router
