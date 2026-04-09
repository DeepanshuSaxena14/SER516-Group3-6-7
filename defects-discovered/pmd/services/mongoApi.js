const MONGO_API_URL = process.env.MONGO_API_URL;

export const getDefectsByRepo = async (repoName) => {
  const res = await fetch(`${MONGO_API_URL}/defects/latest/${encodeURIComponent(repoName)}`);
  if (res.status === 404) return [];
  if (!res.ok) throw new Error(`Failed to fetch defects: ${res.statusText}`);
  return res.json();
};

export const createDefect = async (repoName, filepath, rule, message) => {
  return fetch(`${MONGO_API_URL}/defects`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repoName, filepath, rule, message })
  });
};

export const markDefectsFixed = async (defectIds) => {
  return fetch(`${MONGO_API_URL}/defects/fix`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ defectIds })
  });
};

export const saveDefectCount = async (repoName, totalCount) => {
  return fetch(`${MONGO_API_URL}/defects/count`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repoName, totalCount })
  });
};
