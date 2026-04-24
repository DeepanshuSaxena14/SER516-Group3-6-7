import mongoose from "mongoose";

const StatSchema = new mongoose.Schema({
  sprintName: {
    type: String,
    required: false
  },
  sprintStartDate: {
    type: Date,
    required: false
  },
  sprintEndDate: {
    type: Date,
    required: false
  },
  workCapacity : Number,
  velocity: {
    type: Number,
    required: false,
    description: "Sum of original estimates of all accepted work items"
  },
  boardNumber: Number,
  scannedAt: {
    type: Date,
    default: Date.now
  }
});

export default mongoose.model("Stat", StatSchema);
