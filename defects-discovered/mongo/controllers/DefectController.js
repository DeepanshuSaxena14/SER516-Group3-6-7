import Defect from "../models/DefectModel.js";

// returns defects and counts them
export const aggregateDefects = (violations) => {
  return (violations || []).length;
};

// saves defect count
export const saveDefects = async (totalCount) => {
  try {
    const defect = new Defect({ totalCount });
    const saved = await defect.save();
    console.log(`Saved defect record: ${saved.totalCount} bugs`);
    return saved;
  } catch (error) {
    console.error("Error saving defects:", error);
    throw error;
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