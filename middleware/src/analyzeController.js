import axios from "axios";

const G7_PMD_URL = process.env.G7_PMD_URL || "http://g7-pmd:4000";
const G6_METRICS_URL = process.env.G6_METRICS_URL || "http://g6-metrics:8080";

export const analyzeRepo = async (req, res) => {
    const { github_link } = req.body;

    if (!github_link) {
        return res.status(400).json({ error: "Missing github_link in request body" });
    }

    console.log(`Starting analysis for: ${github_link}`);

    const results = {
        github_link,
        pmd: null,
        metrics: null,
        errors: [],
    };

    const [pmdResult, metricsResult] = await Promise.allSettled([

        axios.get(
            `${G7_PMD_URL}/api/pmd/analyze?github_link=${github_link}`,
            { timeout: 180000 }
        ),

        axios.get(
            `${G6_METRICS_URL}/metrics/analyze`,
            {
                params: { github_link },
                timeout: 180000
            }
        ),
    ]);

    if (pmdResult.status === "fulfilled") {
        results.pmd = pmdResult.value.data;
        console.log("PMD analysis complete");
    } else {
        results.errors.push({ service: "pmd", error: pmdResult.reason.message });
        console.error("PMD analysis failed:", pmdResult.reason.message);
    }

    if (metricsResult.status === "fulfilled") {
        results.metrics = metricsResult.value.data;
        console.log("Fan-In/Fan-Out analysis complete:", results.metrics);
    } else {
        results.errors.push({ service: "g6-metrics", error: metricsResult.reason.message });
        console.error("Fan-In/Fan-Out analysis failed:", metricsResult.reason.message);
    }

    return res.status(200).json(results);
};
