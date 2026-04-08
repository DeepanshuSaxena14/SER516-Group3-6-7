import cors from "cors";
import express from "express";
import { fileURLToPath } from "node:url";
import { createServiceProxy } from "./src/createServiceProxy.js";
import { loadServiceConfig } from "./src/loadServiceConfig.js";

const PORT = Number(process.env.PORT) || 4002;

export const createApp = () => {
  const app = express();
  const serviceConfig = loadServiceConfig();

  app.use(cors());
  app.use(express.json({ limit: "10mb" }));
  app.use(express.urlencoded({ extended: true }));

  for (const service of serviceConfig.routes) {
    app.use(service.route, createServiceProxy(service));
  }

  app.get("/", (req, res) => {
    res.json({
      message: "Middleware server running",
      port: PORT,
      routes: serviceConfig.routes.map(({ route, api }) => ({ route, api })),
    });
  });

  app.get("/health", (req, res) => {
    res.json({
      status: "ok",
      routes: serviceConfig.routes.map(({ route }) => route),
    });
  });

  app.use((req, res) => {
    res.status(404).json({
      error: `No middleware service is configured for ${req.path}`,
    });
  });

  return app;
};

export const startServer = () => {
  const app = createApp();

  return app.listen(PORT, () => {
    console.log(`Middleware running on port ${PORT}`);
  });
};

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  startServer();
}
