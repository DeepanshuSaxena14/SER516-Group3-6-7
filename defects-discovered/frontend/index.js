import { cloneRepo } from "./services/apis/Github.js";

const form = document.querySelector(".github-form form");

async function handleSubmit(event) {
  event.preventDefault();

  const githubLink = document.getElementById("github-link").value;

  try {
    await cloneRepo(githubLink);
    console.log("Repository cloned successfully");
  } 
  catch (error) {
    console.error("Failure in github repo clone form submit");
  }
}

form.addEventListener("submit", handleSubmit);
