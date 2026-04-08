import Defect from "../models/DefectModel.js";
import DefectCount from "../models/DefectCountModel.js";

// returns defects and counts them
export const aggregateDefects = (violations) => {
  return (violations || []).length;
};

export const saveDefectCount = async (req, res) => {
  try {
    const { repoName, totalCount } = req.body;

    if (!repoName || !totalCount) {
      return res.status(400).json({ message: "repoName and totalCount are required" });
    }

    const defectCount = new DefectCount({ repoName, totalCount });
    const saved = await defectCount.save();
    res.status(201).json({ message: "Defect count saved successfully", defectCount: saved });
  } 
  catch (error) {
    console.error("Error saving defect count:", error);
    res.status(500).json({ error: error.message });
  }
};

// pulls the latest defect count
export const getLatestDefect = async (req, res) => {
  try {
    const latest = await Defect.findOne().sort({ analyzedAt: -1 });
    if (!latest) {
      return res.status(404).json({ message: "No defect analysis found" });
    }
    res.status(200).json(latest);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// creates a new defect
export const createDefect = async (req, res) => {
  try {
    const { rule, message } = req.body;

    if (!rule || !message) {
      return res.status(400).json({ message: "rule and message are required" });
    }

    const newDefect = new Defect({
      rule,
      message
    });

    const saved = await newDefect.save();
    res.status(201).json({ message: "Defect created successfully", defect: saved });
  } catch (error) {
    console.error("Error creating defect:", error);
    res.status(500).json({ error: error.message });
  }
};