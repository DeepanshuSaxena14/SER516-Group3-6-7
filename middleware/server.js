import express from "express";
import cors from "cors";
import mongoRoutes from "./routes/mongoRoutes.js";

const PORT = 4002;
const app = express();

app.use(cors());
app.use(express.json());

// Middleware routes
app.use("/mongo", mongoRoutes);

// Health check
app.get("/", (req, res) => {
  res.send("Middleware server running");
});

app.listen(PORT, () => {
  console.log(`Middleware running on port ${PORT}`);
});
