import { cloneRepo } from "./services/apis/Github.js";
import { requestNotificationPermission, sendNotification } from "./services/notifications/NotificationService.js";

const form = document.querySelector(".github-form form");

async function handleSubmit(event) {
  event.preventDefault();

  // Try to request notification permissions upon form submit gesture
  await requestNotificationPermission();

  const githubLink = document.getElementById("github-link").value;

  try {
    await cloneRepo(githubLink);
    console.log("Repository cloned successfully");
    sendNotification("Analysis Complete", {
      body: `The repository has been successfully processed.`
    });
  } 
  catch (error) {
    console.error("Failure in github repo clone form submit");
    sendNotification("Analysis Failed", {
      body: `There was an error processing the repository.`
    });
  }
}

form.addEventListener("submit", handleSubmit);
