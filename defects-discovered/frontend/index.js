import { cloneRepo } from "./services/apis/Github.js";
import { requestNotificationPermission, sendNotification } from "./services/notifications/NotificationService.js";

const form = document.querySelector(".github-form form");
const submitBtn = form.querySelector('button[type="submit"]');

async function handleSubmit(event) {
  event.preventDefault();

  // Try to request notification permissions upon form submit gesture
  await requestNotificationPermission();

  const githubLink = document.getElementById("github-link").value;

  const originalText = submitBtn.textContent;
  submitBtn.textContent = "Processing...";
  submitBtn.disabled = true;

  try {
    await cloneRepo(githubLink);
    console.log("Repository cloned successfully");
    sendNotification("Analysis Complete", {
      body: `The repository has been successfully processed.`
    });
    alert("Analysis Complete: The repository has been successfully processed.");
  } 
  catch (error) {
    console.error("Failure in github repo clone form submit");
    sendNotification("Analysis Failed", {
      body: `There was an error processing the repository.`
    });
    alert("Analysis Failed: Could not reach the backend API. Please make sure the backend server (port 4000) is running.");
  } finally {
    submitBtn.textContent = originalText;
    submitBtn.disabled = false;
  }
}

form.addEventListener("submit", handleSubmit);

