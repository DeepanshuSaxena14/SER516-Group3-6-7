import axios from "axios";

const TAIGA_SERVICE_URL = process.env.TAIGA_SERVICE_URL || "http://taiga-service:8080";

export const getStories = async (req, res) => {
    const { project_id, sprint_id } = req.query;
    if (!project_id || !sprint_id) {
        return res.status(400).json({ error: "Missing project_id or sprint_id" });
    }
    try {
        const response = await axios.get(`${TAIGA_SERVICE_URL}/taiga/stories`, {
            params: { project_id, sprint_id },
            timeout: 30000
        });
        return res.status(200).json(response.data);
    } catch (e) {
        return res.status(500).json({ error: e.message });
    }
};

export const getSprint = async (req, res) => {
    const { sprint_id } = req.query;
    if (!sprint_id) {
        return res.status(400).json({ error: "Missing sprint_id" });
    }
    try {
        const response = await axios.get(`${TAIGA_SERVICE_URL}/taiga/sprint`, {
            params: { sprint_id },
            timeout: 30000
        });
        return res.status(200).json(response.data);
    } catch (e) {
        return res.status(500).json({ error: e.message });
    }
};