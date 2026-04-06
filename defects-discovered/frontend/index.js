import { cloneRepo } from "./services/apis/Github.js";

const form = document.querySelector(".github-form form");
const githubLinkInput = document.getElementById("github-link");
const submitButton = form.querySelector('button[type="submit"]');
const status = document.getElementById("analysis-status");
const resultPlaceholder = document.getElementById("result-placeholder");
const defaultButtonLabel = submitButton.textContent;

function setLoadingState(isLoading) {
  status.classList.toggle("hidden", !isLoading);
  githubLinkInput.disabled = isLoading;
  submitButton.disabled = isLoading;
  submitButton.textContent = isLoading ? "Analyzing..." : defaultButtonLabel;
}

async function handleSubmit(event) {
  event.preventDefault();

  const githubLink = githubLinkInput.value;

  setLoadingState(true);
  resultPlaceholder.classList.add("hidden");

  try {
    await cloneRepo(githubLink);
    resultPlaceholder.classList.remove("hidden");
    console.log("Repository cloned successfully");
  } 
  catch (error) {
    console.error("Failure in github repo clone form submit");
  }
  finally {
    setLoadingState(false);
  }
}

form.addEventListener("submit", handleSubmit);
