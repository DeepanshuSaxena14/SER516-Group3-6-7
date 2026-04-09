import pmdHttp from "../../plugins/Http.js";

export const cloneRepo = async (URL) => {
  const path = "/pmd/analyze";

  const options = {
    method: "GET",
    url: path,
    params: { github_link: URL },
  };

  try {
    return await pmdHttp(options);
  } catch (error) {
    console.error("Failure in cloneRepo api call");
    console.error(error);
    throw new Error();
  }
};
