import mongoose from "mongoose";

const DefectSchema = new mongoose.Schema({
  totalCount: Number,
  analyzedAt: {
    type: Date,
    default: Date.now
  }
});

export default mongoose.model("Defect", DefectSchema);