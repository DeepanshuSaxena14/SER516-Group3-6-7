import axios from "axios";

const MONGO_SERVICE_URL = "http://localhost:4001/api"; // your Mongo service

export const createFocusFactor = async (req, res) => {
  try {
    const response = await axios.post(`${MONGO_SERVICE_URL}/stats`, req.body);
    res.status(response.status).json(response.data);
  } catch (error) {
    res.status(error.response?.status || 500).json({ error: error.message });
  }
};

export const getStats = async (req, res) => {
  try {
    const response = await axios.get(`${MONGO_SERVICE_URL}/stats`);
    res.status(response.status).json(response.data);
  } catch (error) {
    res.status(error.response?.status || 500).json({ error: error.message });
  }
};

export const updateStat = async (req, res) => {
  try {
    const { id } = req.params;
    const response = await axios.put(
      `${MONGO_SERVICE_URL}/stats/${id}`,
      req.body,
    );
    res.status(response.status).json(response.data);
  } catch (error) {
    res.status(error.response?.status || 500).json({ error: error.message });
  }
};

export const deleteStatById = async (req, res) => {
  try {
    const { id } = req.params;
    const response = await axios.delete(`${MONGO_SERVICE_URL}/stats/${id}`);
    res.status(response.status).json(response.data);
  } catch (error) {
    res.status(error.response?.status || 500).json({ error: error.message });
  }
};
