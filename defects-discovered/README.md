# SER516 Java Defects Detector

## Backend
- We have 2 micro services
   - pmd (defect analyzer)
   - mongo (database)


## Mongo

### How to run Mongo

```bash
cd mongo

npm install

npm run start

```

### endpoints
#### stats (Focus factor entry)

gets all stats
get `/api/stat `

create stat
post `/api/stat`

update stat
put `/api/stat/:id`

delete stat
delete `/api/stat/:id`

example json body for focus factor create and update stat:
```JSON
{
  "workCapacity": 80,
  "velocity": 45
}
```


## PMD + frontend With Docker

**Only the pmd and frontend work with docker - Mongo, the unit tests and static analysis have not been configured**
- Navigate to the root of the project and make sure the docker-compose file is there
- run `docker compose up --build` to run both pmd and the frontend
- Open a browser and type http://localhost:80 to access the frontend
- Enter the repo url that you want to analyze

> [!NOTE]
> This is the same from last sprint, the results are only returned in the api response and not shown in the frontend. This will be later integrated with grafana at a future sprint

### Pmd enpoints:
Run pmd analysis
post `/api/github/clone`

take in a json body of this format:
{ github_link: "URL" }

## PMD
- You can run pmd locally as well
- Make sure you have the pmd instance installed locally, if not then just use the docker method above

```bash
cd pmd

npm install

npm run start
```


## Frontend
- The frontend here just a page to enter a github repo link that pmd analyzes. It is not the main dashboard used for any actual UI or statistics

### How to run front local

```bash
cd frontend

npx http-server -p 80 (or any port you want)

```
Then just open the browser at localhost:80


## Running Unit Tests
- We have unit tests for the pmd and mongo crud services
- To run the unit tests make sure you are in the root of the project and follow these commands

```bash
npm install

npm run test
```

## Running Static Analysis
- We are using eslint to run static analysis on all the project files
- To run, make sure you are in the root of the project and follow these command

```bash
npm install

npm run lint
```
Any errors or warning should appear


## Jenkins
- We have automation pipelines set to run the static analysis and unit tests whenever there is a push on the US, Sprint or main branches
- To access Jenkins use this link: https://swent0linux.asu.edu/jenkins/job/Group-7/
- The Static analysis pipeline is called: *Static Analysis on Express Server*
- The Unit tests pipeline is called: *Main_Pipeline_Vitest*


## Note

> [!NOTE]
> - Port 80 is needed to run the frontend
> - Port 4000 to run pmd
> - Port 4001 to run mongo

Any of these ports can be changed if needed from each corresponding env file
