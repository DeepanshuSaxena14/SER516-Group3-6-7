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

// gets all unfixed defects for a repo
export const getLatestDefectDetails = async (req, res) => {
  try {
    const { repoName } = req.params;
    const defects = await Defect.find({ repoName, isFixed: false }).sort({ analyzedAt: -1 });
    
    if (defects.length === 0) {
      return res.status(404).json({ message: `No unfixed defects found for repo '${repoName}'` });
    }
    
    res.status(200).json(defects);
  } 
  catch (error) {
    res.status(500).json({ error: error.message });
  }
};

export const getLatestRepoDefectDetails = async (req, res) => {
  try {
    const latest = await DefectCount.findOne().sort({ analyzedAt: -1 });

    if (!latest) {
      return res.status(404).json({ message: "No analysis data found" });
    }

    const defects = await Defect.find({ repoName: latest.repoName, isFixed: false }).sort({ analyzedAt: -1 });

    if (defects.length === 0) {
      return res.status(404).json({ message: `No unfixed defects found for repo '${latest.repoName}'` });
    }

    res.status(200).json(defects);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

export const markDefectsFixed = async (req, res) => {
  try {
    const { defectIds } = req.body;

    if (!defectIds || !Array.isArray(defectIds) || defectIds.length === 0) {
      return res.status(400).json({ message: "defectIds array is required" });
    }

    const result = await Defect.updateMany(
      { _id: { $in: defectIds } },
      { $set: { isFixed: true } }
    );

    res.status(200).json({ message: "Defects marked as fixed", modifiedCount: result.modifiedCount });
  } catch (error) {
    console.error("Error marking defects as fixed:", error);
    res.status(500).json({ error: error.message });
  }
};

const formatDate = (date) => {
  return new Date(date).toLocaleString("en-US", {
    timeZone: "America/Phoenix",
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: true
  });
};

export const getLatestRepoSummary = async (req, res) => {
  try {
    const latest = await DefectCount.findOne().sort({ analyzedAt: -1 });

    if (!latest) {
      return res.status(404).json({ message: "No analysis data found" });
    }

    const filesAnalyzed = await Defect.distinct("filepath", { repoName: latest.repoName });

    res.status(200).json({
      repoName: latest.repoName,
      defectCount: latest.totalCount,
      filesAnalyzed: filesAnalyzed.length,
      analyzedAt: formatDate(latest.analyzedAt)
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

export const getLatestRepoDefectCounts = async (req, res) => {
  try {
    const latest = await DefectCount.findOne().sort({ analyzedAt: -1 });

    if (!latest) {
      return res.status(404).json({ message: "No analysis data found" });
    }

    const counts = await DefectCount.find({ repoName: latest.repoName }).sort({ analyzedAt: -1 });

    res.status(200).json(counts.map(c => ({
      repoName: c.repoName,
      totalCount: c.totalCount,
      analyzedAt: formatDate(c.analyzedAt),
      analyzedAtDate: new Date(c.analyzedAt).toLocaleDateString("en-CA", { timeZone: "America/Phoenix" }),
      analyzedAtISO: c.analyzedAt
    })));
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// creates a new defect
export const createDefect = async (req, res) => {
  try {
    const { repoName, filepath, rule, message } = req.body;

    if (!repoName || !filepath || !rule || !message) {
      return res.status(400).json({ message: "repoName, filepath, rule, and message are required" });
    }

    const newDefect = new Defect({
      repoName,
      filepath,
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