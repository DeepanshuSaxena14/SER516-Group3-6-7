import mongoose from "mongoose";

const DefectSchema = new mongoose.Schema({
  totalCount: Number,
  rule: String,
  message: String,
  analyzedAt: {
    type: Date,
    default: Date.now
  }
});

export default mongoose.model("Defect", DefectSchema);