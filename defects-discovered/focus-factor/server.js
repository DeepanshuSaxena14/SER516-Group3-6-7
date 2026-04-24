import express from "express";
import dotenv from "dotenv";
import cors from "cors";

dotenv.config();

const PORT = process.env.FOCUS_FACTOR_PORT || 4002;
const app = express();

app.use(cors());
app.use(express.json());

app.get("/health", (req, res) => {
  res.status(200).type("text/plain").send("Focus Factor service is running");
});

app.listen(PORT, () => console.log(`Focus Factor service running on port ${PORT}`));

export default app;
