import { createProxyMiddleware, fixRequestBody } from "http-proxy-middleware";

export const createServiceProxy = (service) =>
  createProxyMiddleware({
    target: service.api,
    changeOrigin: true,
    on: {
      proxyReq: fixRequestBody,
      error: (error, req, res) => {
        if (res.headersSent) {
          return;
        }

        res.status(502).json({
          error: `Failed to reach upstream service for ${service.route}`,
          target: `${service.api}${req.url === "/" ? "" : req.url}`,
          details: error.message,
        });
      },
    },
  });
