import ENV from "../env.js";

const pmdHttp = axios.create({
  baseURL: `${ENV.PMD_BASE_URL}`,
  headers: {
    "Content-Type": "application/json",
  },
});

export default pmdHttp;