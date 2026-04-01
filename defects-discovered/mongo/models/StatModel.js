import mongoose from "mongoose";

const StatSchema = new mongoose.Schema({
  workCapacity : Number,
  velocity : Number,
  boardNumber : Number,
  scannedAt: {
    type: Date,
    default: Date.now
  }
});

export default mongoose.model("Stat", StatSchema);
