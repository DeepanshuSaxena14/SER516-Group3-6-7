import mongoose from "mongoose";

const DefectCountSchema = new mongoose.Schema({
  repoName: {
    type: String,
    required: true
  },
  totalCount: {
    type: Number,
    required: true
  },
  analyzedAt: {
    type: Date,
    default: Date.now
  }
});

export default mongoose.model("DefectCount", DefectCountSchema);
