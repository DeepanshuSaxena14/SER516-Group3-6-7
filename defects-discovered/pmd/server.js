import express from "express"
import dotenv from "dotenv"
import cors from "cors"
import githubRoutes from "./routes/GithubRoutes.js"
import pmdRoutes from "./routes/PmdRoutes.js"
import client from "prom-client"

dotenv.config()

const PORT = process.env.PORT || 4000
const server = express()

const register = new client.Registry()
client.collectDefaultMetrics({ register })

server.use(cors())
server.use(express.json())

server.get("/prometheus", async (req, res) => {
  res.set("Content-Type", register.contentType)
  res.end(await register.metrics())
})

server.use("/api/github", githubRoutes)
server.use("/api/pmd", pmdRoutes)

server.listen(PORT, () => {
  console.log(`Running on port ${PORT}`)
  console.log(`Prometheus metrics available at http://localhost:${PORT}/prometheus`)
})

export default server
