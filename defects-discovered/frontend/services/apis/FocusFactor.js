import { mongoHttp } from "../../plugins/Http.js";

export const createStat = async (velocity, workCapacity) => {
  const options = {
    method: "POST",
    url: "/stats",
    data: { velocity, workCapacity }
  };

  try {
    const response = await mongoHttp(options);
    return response.data;
  } catch (error) {
    console.error("Failure in createStat api call");
    console.error(error);
    throw new Error(`Failed to create stat: ${error.message}`);
  }
};

export const getStats = async () => {
  const options = {
    method: "GET",
    url: "/stats"
  };

  try {
    const response = await mongoHttp(options);
    return response.data;
  } catch (error) {
    console.error("Failure in getStats api call");
    console.error(error);
    throw new Error(`Failed to fetch stats: ${error.message}`);
  }
};

export const updateStat = async (id, stat) => {
  const options = {
    method: "PUT",
    url: `/stats/${id}`,
    data: stat
  };

  try {
    const response = await mongoHttp(options);
    return response.data;
  } catch (error) {
    console.error("Failure in updateStat api call");
    console.error(error);
    throw new Error(`Failed to update stat: ${error.message}`);
  }
};

export const deleteStat = async (id) => {
  const options = {
    method: "DELETE",
    url: `/stats/${id}`
  };

  try {
    const response = await mongoHttp(options);
    return response.data;
  } catch (error) {
    console.error("Failure in deleteStat api call");
    console.error(error);
    throw new Error(`Failed to delete stat: ${error.message}`);
  }
};
