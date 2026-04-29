import pkg from "pg";
const { Pool } = pkg;

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

export const getVelocities = async (req, res) => {
  const { projectId } = req.params;

  const token = process.env.TAIGA_BEARER_TOKEN;
  const apiBaseUrl = process.env.TAIGA_API_BASE_URL || "https://api.taiga.io";
  const apiV1 = `${apiBaseUrl}/api/v1`;

  if (!projectId) {
    return res.status(400).json({ error: "projectId is required" });
  }

  if (!token) {
    return res.status(500).json({ error: "Missing TAIGA_BEARER_TOKEN" });
  }

  const headers = {
    Authorization: `Bearer ${token}`,
    Accept: "application/json",
  };

  try {
    // Fetch all sprints (milestones)
    const sprintsRes = await fetch(
      `${apiV1}/milestones?project=${projectId}`,
      { headers }
    );

    if (!sprintsRes.ok) {
      const text = await sprintsRes.text().catch(() => "");
      return res.status(sprintsRes.status).json({
        error: "Failed to fetch sprints from Taiga",
        details: text || undefined,
      });
    }

    const sprints = await sprintsRes.json();

    // Fetch user stories for all sprints in parallel and compute velocities
    const velocities = await Promise.all(
      sprints.map(async (sprint) => {
        const storiesRes = await fetch(
          `${apiV1}/userstories?project=${projectId}&milestone=${sprint.id}`,
          { headers }
        );

        const stories = storiesRes.ok ? await storiesRes.json() : [];
        const velocity = (stories || [])
          .filter((s) => s.is_closed === true)
          .reduce((sum, s) => sum + (s.total_points ?? s.estimated_points ?? 0), 0);

        return {
          sprintName: sprint.name,
          sprintId: sprint.id,
          sprintStart: sprint.estimated_start,
          sprintEnd: sprint.estimated_finish,
          velocity,
        };
      })
    );

    await Promise.all(
      velocities.map((v) =>
        pool.query(
          `
          INSERT INTO sprint_velocity
            (project_id, sprint_id, sprint_name, sprint_start_date, sprint_end_date, velocity)
          VALUES ($1, $2, $3, $4, $5, $6)
          ON CONFLICT (project_id, sprint_id)
          DO UPDATE SET
            sprint_name       = EXCLUDED.sprint_name,
            sprint_start_date = EXCLUDED.sprint_start_date,
            sprint_end_date   = EXCLUDED.sprint_end_date,
            velocity          = EXCLUDED.velocity
          `,
          [projectId, v.sprintId, v.sprintName, v.sprintStart, v.sprintEnd, v.velocity]
        )
      )
    );

    // 4. Respond
    return res.status(200).json({
      projectId,
      velocities,
      saved: velocities.length,
    });
  } catch (err) {
    console.error("Velocity error:", err);
    return res.status(500).json({
      error: "Failed to compute or save velocities",
      details: err instanceof Error ? err.message : String(err),
    });
  }
};