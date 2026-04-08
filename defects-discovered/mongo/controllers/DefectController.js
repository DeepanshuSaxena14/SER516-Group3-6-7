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

export const getLatestDefectCount = async (req, res) => {
  try {
    const { repoName } = req.params;
    const latest = await DefectCount.findOne({ repoName }).sort({ analyzedAt: -1 });
    
    if (!latest) {
      return res.status(404).json({ message: `No defect count found for repo '${repoName}'` });
    }
    
    res.status(200).json(latest);
  } 
  catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// gets latest defect details for a repo
export const getLatestDefectDetails = async (req, res) => {
  try {
    const { repoName } = req.params;
    const latest = await Defect.findOne({ repoName }).sort({ analyzedAt: -1 });
    
    if (!latest) {
      return res.status(404).json({ message: `No defects found for repo '${repoName}'` });
    }
    
    res.status(200).json(latest);
  } 
  catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// creates a new defect
export const createDefect = async (req, res) => {
  try {
    const { repoName, rule, message } = req.body;

    if (!repoName || !rule || !message) {
      return res.status(400).json({ message: "repoName, rule, and message are required" });
    }

    const newDefect = new Defect({
      repoName,
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