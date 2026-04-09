import express from "express"
import dotenv from "dotenv"
import cors from "cors"
import mongoose from "mongoose";
import crudRoutes from "./routes/CrudRoutes.js";
import defectRoutes from "./routes/DefectRoutes.js";

dotenv.config()

const PORT = process.env.PORT || 4001
const server = express()

server.use(cors())
server.use(express.json())

server.use("/api", crudRoutes);
server.use("/api", defectRoutes);

// MongoDB connection
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log("MongoDB connected"))
  .catch(err => console.log(err));

// Health check route
server.get("/health", (req, res) => {
  res.status(200).type("text/plain").send("Server works");
  console.log("Server works")
});


server.listen(PORT, () => console.log(`Running on port ${PORT}`))

