export const getVelocities = async (req, res) => {
  const { projectId } = req.params;
  const token = process.env.TAIGA_BEARER_TOKEN;
  const apiBaseUrl = process.env.TAIGA_API_BASE_URL || "https://api.taiga.io";

  if (!token) {
    return res.status(500).json({ error: "Missing Taiga auth token (set TAIGA_BEARER_TOKEN)" });
  }

  try {
    // Fetch all sprints
    const sprintsRes = await fetch(`${apiBaseUrl}/api/v1/milestones?project=${projectId}`, {
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
    const velocities = [];

    // For each sprint, fetch user stories and calculate velocity
    for (const sprint of sprints) {
      try {
        const storiesRes = await fetch(
          `${apiBaseUrl}/api/v1/userstories?project=${projectId}&milestone=${sprint.id}`,
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

        velocities.push({
          sprintName: sprint.name,
          sprintId: sprint.id,
          sprintStart: sprint.estimated_start,
          sprintEnd: sprint.estimated_finish,
          velocity,
        });
      } catch (sprintError) {
        console.error(`Error processing sprint ${sprint.name}:`, sprintError.message);
      }
    }

    res.status(200).json({
      projectId,
      velocities,
    });
  } catch (err) {
    return res.status(502).json({
      error: "Error contacting Taiga",
      details: err instanceof Error ? err.message : String(err),
    });
  }
};
