import express from "express";
import cors from "cors";
import githubRoutes from "./routes/GithubRoutes.js";

export function createApp() {
    const app = express();

    app.use(cors());
    app.use(express.json());

    app.use("/api/github", githubRoutes);

    return app;
}

export default createApp();