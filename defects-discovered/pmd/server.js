import express from "express"
import dotenv from "dotenv"
import cors from "cors"
import githubRoutes from "./routes/GithubRoutes.js"
import pmdRoutes from "./routes/PmdRoutes.js"

dotenv.config()

const PORT = process.env.PORT || 8080 
const server = express()

server.use(cors())
server.use(express.json())

server.use("/api/github", githubRoutes)
server.use("/api/pmd", pmdRoutes)

server.listen(PORT, () => console.log(`Running on port ${PORT}`))

