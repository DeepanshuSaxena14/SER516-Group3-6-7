import ENV from "../../env.js";

export const cloneRepo = async (URL) => {
  try {
    const response = await axios.post(
      ENV.ANALYZE_URL,
      { github_link: URL },
      { timeout: 300000 }
    );
    return response.data;
  } catch (error) {
    console.error("Analysis request failed:", error);
    throw new Error(error.response?.data?.error || "Analysis failed");
  }
};
