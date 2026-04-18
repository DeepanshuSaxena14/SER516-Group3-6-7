import { Router } from "express"
import { createFocusFactor, getStats, updateStat, deletStatbyId, saveSprintVelocity, getSprintVelocities } from "../controllers/CrudController.js";

const router = Router()

router.post("/stats", createFocusFactor);
router.get("/stats", getStats);
router.put("/stats/:id", updateStat);
router.delete("/stats/:id", deletStatbyId);

router.post("/sprint-velocities", saveSprintVelocity);
router.get("/sprint-velocities", getSprintVelocities);

export default router
