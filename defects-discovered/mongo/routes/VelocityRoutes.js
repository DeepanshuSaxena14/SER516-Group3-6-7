import { Router } from "express";
import { calculateAndSaveSprintVelocities } from "../controllers/VelocityController.js";

const router = Router();

router.post("/calculate-velocities", calculateAndSaveSprintVelocities);

export default router;
