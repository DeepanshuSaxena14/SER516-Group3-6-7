export const getCapacity = async (req, res) => {
    const { projectId } = req.params;
    const token = process.env.TAIGA_BEARER_TOKEN;
    const apiBaseUrl = process.env.TAIGA_API_BASE_URL || "https://api.taiga.io/";

    if (!token) {
        return res.status(500).json({ error: "Missing Taiga auth token (set TAIGA_BEARER_TOKEN)" });
    }

    try {
        const projectStatsRes = await fetch(`${apiBaseUrl}/api/v1/projects/${projectId}/stats`, {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: "application/json",
            },
        });

        if (!projectStatsRes.ok) {
            const text = await projectStatsRes.text().catch(() => "");
            return res.status(projectStatsRes.status).json({
                error: "Failed to fetch Taiga project stats",
                details: text || undefined,
            });
        }

        const projectStatsData = await projectStatsRes.json();
        const capacity = projectStatsData?.assigned_points;
        return res.status(200).json({ capacity });
    } catch (err) {
        return res.status(502).json({
            error: "Error contacting Taiga",
            details: err instanceof Error ? err.message : String(err),
        });
    }
}