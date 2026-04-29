import { Router } from "express";
import { getVelocities } from "../controllers/VelocityController.js";

const router = Router();

router.get("/velocity/:projectId", getVelocities);

export default router;
