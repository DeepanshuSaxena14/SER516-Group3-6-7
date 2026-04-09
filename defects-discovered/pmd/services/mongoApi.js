const MONGO_API_URL = process.env.MONGO_API_URL;

export const createDefect = async (repoName, rule, message) => {
  return fetch(`${MONGO_API_URL}/defects`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repoName, rule, message })
  });
};

export const saveDefectCount = async (repoName, totalCount) => {
  return fetch(`${MONGO_API_URL}/defects/count`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repoName, totalCount })
  });
};
