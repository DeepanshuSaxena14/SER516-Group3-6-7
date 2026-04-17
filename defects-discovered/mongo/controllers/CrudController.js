import Stat from "../models/StatModel.js";
import StatSchema from "../models/StatModel.js";

export const createFocusFactor = async (req, res) => {
  try {
    const { velocity, workCapacity } = req.body;

    const stat = new StatSchema({
      velocity,
      workCapacity,
    });

    const saved = await stat.save();

    res.status(201).json(saved);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// READ (GET ALL STATS)
export const getStats = async (req, res) => {
  try {
    const stats = await Stat.find();
    res.status(200).json(stats);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// UPDATE
export const updateStat = async (req, res) => {
  try {
    const { id } = req.params;
    const updated = await Stat.findByIdAndUpdate(id, req.body, { new: true });
    if (!updated) return res.status(404).json({ error: "Stat not found" });
    res.status(200).json(updated);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// DELETE
export const deletStatbyId = async (req, res) => {
  try {
    const { id } = req.params;
    const deleted = await Stat.findByIdAndDelete(id);

    if (!deleted) return res.status(404).json({ error: "Stat not found" });
    
    res.status(200).json(deleted);
  } 
  catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// SAVE SPRINT VELOCITY
export const saveSprintVelocity = async (req, res) => {
  try {
    const { sprintName, sprintStartDate, sprintEndDate, velocity } = req.body;

    if (!sprintName || !velocity) {
      return res.status(400).json({ error: "sprintName and velocity are required" });
    }

    const stat = new StatSchema({
      sprintName,
      sprintStartDate: sprintStartDate ? new Date(sprintStartDate) : null,
      sprintEndDate: sprintEndDate ? new Date(sprintEndDate) : null,
      velocity,
    });

    const saved = await stat.save();
    res.status(201).json(saved);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// GET ALL SPRINT VELOCITIES
export const getSprintVelocities = async (req, res) => {
  try {
    const velocities = await Stat.find({ sprintName: { $exists: true, $ne: null } }).sort({ sprintStartDate: -1 });
    res.status(200).json(velocities);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
