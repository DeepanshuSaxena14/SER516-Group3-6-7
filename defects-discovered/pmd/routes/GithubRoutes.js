import { Router } from "express"
import { cloneRepo } from "../controllers/GithubController.js" 

const router = Router()

router.post("/clone", cloneRepo)

export default router