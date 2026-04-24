import express from "express";
import dotenv from "dotenv";
import cors from "cors";

dotenv.config();

const PORT = process.env.FOCUS_FACTOR_PORT || 4002;
const MONGO_SERVICE_URL = process.env.MONGO_SERVICE_URL || "http://localhost:4001";
const app = express();

app.use(cors());
app.use(express.json());

app.get("/health", (req, res) => {
  res.status(200).type("text/plain").send("Focus Factor service is running");
});

app.get("/focus-factor/:boardNumber", async (req, res) => {
  const boardNumber = parseInt(req.params.boardNumber, 10);

  if (isNaN(boardNumber)) {
    return res.status(400).json({ error: "boardNumber must be an integer" });
  }

  let stats;
  try {
    const response = await fetch(`${MONGO_SERVICE_URL}/api/stats`);
    if (!response.ok) {
      return res.status(502).json({ error: "Failed to fetch stats from mongo service" });
    }
    stats = await response.json();
  } catch (err) {
    return res.status(502).json({ error: "Could not reach mongo service", details: err.message });
  }

  const boardStats = stats.filter((s) => s.boardNumber === boardNumber);

  if (boardStats.length === 0) {
    return res.status(404).json({ error: `No stats found for boardNumber ${boardNumber}` });
  }

  const results = boardStats.map((s) => {
    const focusFactor =
      s.workCapacity > 0 ? parseFloat((s.velocity / s.workCapacity).toFixed(4)) : null;
    return {
      sprintName: s.sprintName,
      sprintStartDate: s.sprintStartDate,
      sprintEndDate: s.sprintEndDate,
      velocity: s.velocity,
      workCapacity: s.workCapacity,
      focusFactor,
    };
  });

  res.status(200).json({ boardNumber, focusFactors: results });
});

app.listen(PORT, () => console.log(`Focus Factor service running on port ${PORT}`));

export default app;
