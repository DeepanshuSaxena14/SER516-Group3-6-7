import mongoose from "mongoose";

const DefectSchema = new mongoose.Schema({
  repoName: {
    type: String,
    required: true
  },
  isFixed: {
    type: Boolean,
    default: false
  },
  rule: String,
  message: String,
  analyzedAt: {
    type: Date,
    default: Date.now
  }
});

export default mongoose.model("Defect", DefectSchema);