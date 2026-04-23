import { cloneRepo } from "./services/apis/Github.js";
import { requestNotificationPermission, sendNotification } from "./services/notifications/NotificationService.js";

const form = document.querySelector(".github-form form");
const githubLinkInput = document.getElementById("github-link");
const submitButton = form.querySelector('button[type="submit"]');
const status = document.getElementById("analysis-status");
const resultPlaceholder = document.getElementById("result-placeholder");
const defaultButtonLabel = submitButton.textContent;

const GRAFANA_URL = "https://swent0linux.asu.edu/grafana/";

function setLoadingState(isLoading) {
  status.classList.toggle("hidden", !isLoading);
  githubLinkInput.disabled = isLoading;
  submitButton.disabled = isLoading;
  submitButton.textContent = isLoading ? "Analyzing..." : defaultButtonLabel;
}

async function handleSubmit(event) {
  event.preventDefault();

  // Request notification permission on user gesture (form submit)
  await requestNotificationPermission();

  const githubLink = githubLinkInput.value.trim();

  if (!githubLink) return;

  setLoadingState(true);
  resultPlaceholder.classList.add("hidden");

  try {
    const result = await cloneRepo(githubLink);

    if (!result || result.message?.toLowerCase().includes("failed")) {
      throw new Error(result?.message || "Analysis failed");
    }

    const resultLink = resultPlaceholder.querySelector(".result-link");
    resultLink.href = GRAFANA_URL;
    resultLink.textContent = "View Dashboards on Grafana →";
    resultLink.target = "_blank";
    resultPlaceholder.classList.remove("hidden");

    console.log("Analysis complete:", result);
    sendNotification("Analysis Complete", {
      body: "The repository has been successfully processed. View results in Grafana."
    });
  } catch (error) {
    console.error("Analysis failed:", error.message);

    const resultLink = resultPlaceholder.querySelector(".result-link");
    resultLink.href = "#";
    resultLink.textContent = "Analysis failed — check console for details";
    resultPlaceholder.classList.remove("hidden");

    sendNotification("Analysis Failed", {
      body: "There was an error processing the repository. Check the console for details."
    });
  } finally {
    setLoadingState(false);
  }
}

form.addEventListener("submit", handleSubmit);
