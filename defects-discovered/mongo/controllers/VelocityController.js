import Stat from "../models/StatModel.js";

export const calculateAndSaveSprintVelocities = async (req, res) => {
  const { projectId } = req.body;
  const token = process.env.TAIGA_BEARER_TOKEN;

  if (!projectId) {
    return res.status(400).json({ error: "projectId is required" });
  }

  if (!token) {
    return res.status(500).json({ error: "Missing Taiga auth token (set TAIGA_BEARER_TOKEN)" });
  }

  try {
    // Fetch all sprints
    const sprintsRes = await fetch(`https://api.taiga.io/api/v1/milestones?project=${projectId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    });

    if (!sprintsRes.ok) {
      const text = await sprintsRes.text().catch(() => "");
      return res.status(sprintsRes.status).json({
        error: "Failed to fetch Taiga sprints",
        details: text || undefined,
      });
    }

    const sprints = await sprintsRes.json();
    const savedVelocities = [];

    // For each sprint, fetch user stories and calculate velocity
    for (const sprint of sprints) {
      try {
        const storiesRes = await fetch(
          `https://api.taiga.io/api/v1/userstories?project=${projectId}&milestone=${sprint.id}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
              Accept: "application/json",
            },
          }
        );

        if (!storiesRes.ok) continue;

        const stories = await storiesRes.json();

        // Calculate velocity: sum of estimated_points for accepted stories
        const velocity = (stories || [])
          .filter((story) => story.status_extra_info?.name?.toLowerCase() === "accepted")
          .reduce((sum, story) => sum + (story.estimated_points || 0), 0);

        // Save to MongoDB
        const stat = new Stat({
          sprintName: sprint.name,
          sprintStartDate: sprint.created_date ? new Date(sprint.created_date) : null,
          sprintEndDate: sprint.estimated_finish ? new Date(sprint.estimated_finish) : null,
          velocity,
        });

        const saved = await stat.save();
        savedVelocities.push(saved);
      } catch (sprintError) {
        console.error(`Error processing sprint ${sprint.name}:`, sprintError.message);
      }
    }

    res.status(200).json({
      message: `Calculated and saved velocities for ${savedVelocities.length} sprints`,
      velocities: savedVelocities,
    });
  } catch (err) {
    return res.status(502).json({
      error: "Error contacting Taiga",
      details: err instanceof Error ? err.message : String(err),
    });
  }
};
