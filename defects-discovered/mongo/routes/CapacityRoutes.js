import { Router } from "express";
import { getCapacity } from "../controllers/CapacityController.js";

const router = Router();

router.get("/capacity/:projectId", getCapacity);

export default router;
