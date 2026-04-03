import { Router } from "express"
import { runPMDAnalysis, cloneAndAnalyzeRepo } from "../controllers/PmdController.js"

const router = Router()




router.post("/run-pmd", runPMDAnalysis)
router.get("/analyze", cloneAndAnalyzeRepo)

export default router