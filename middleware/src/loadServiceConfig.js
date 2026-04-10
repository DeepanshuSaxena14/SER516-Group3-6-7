import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const middlewareDir = path.resolve(currentDir, "..");
const defaultEnvFile = path.join(middlewareDir, ".env");

const normalizeRoute = (route) => {
  if (typeof route !== "string" || !route.trim()) {
    throw new Error("Each service route must include a non-empty route value.");
  }

  const trimmedRoute = route.trim().replace(/^\/+|\/+$/g, "");

  if (!trimmedRoute) {
    throw new Error("Service route cannot resolve to the root path.");
  }

  return `/${trimmedRoute}`;
};

const normalizeApi = (api) => {
  if (typeof api !== "string" || !api.trim()) {
    throw new Error("Each service route must include a non-empty api value.");
  }

  return api.trim().replace(/\/+$/g, "");
};

const parseConfig = (rawConfig, sourceLabel) => {
  let parsedConfig;

  try {
    parsedConfig = JSON.parse(rawConfig);
  } catch (error) {
    throw new Error(`Unable to parse service config from ${sourceLabel}: ${error.message}`);
  }

  if (!parsedConfig || !Array.isArray(parsedConfig.routes)) {
    throw new Error(`Service config from ${sourceLabel} must contain a routes array.`);
  }

  return {
    routes: parsedConfig.routes.map((service, index) => {
      if (!service || typeof service !== "object") {
        throw new Error(`Route entry at index ${index} in ${sourceLabel} must be an object.`);
      }

      return {
        route: normalizeRoute(service.route),
        api: normalizeApi(service.api),
      };
    }),
  };
};

const loadEnvFile = () => {
  if (fs.existsSync(defaultEnvFile)) {
    process.loadEnvFile(defaultEnvFile);
  }
};

export const loadServiceConfig = () => {
  loadEnvFile();

  if (!process.env.SERVICE_ROUTES_JSON) {
    throw new Error(
      "SERVICE_ROUTES_JSON is required in middleware/.env and must contain a JSON object with a routes array.",
    );
  }

  return parseConfig(process.env.SERVICE_ROUTES_JSON, "SERVICE_ROUTES_JSON");
};
