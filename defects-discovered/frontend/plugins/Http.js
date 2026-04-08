import ENV from "../env.js";

const pmdHttp = axios.create({
  baseURL: `${ENV.PMD_BASE_URL}/api`,
  headers: {
    "Content-Type": "application/json",
  },
});

const mongoHttp = axios.create({
  baseURL: `${ENV.MONGO_BASE_URL}/api`,
  headers: {
    "Content-Type": "application/json",
  },
});

export { pmdHttp, mongoHttp };