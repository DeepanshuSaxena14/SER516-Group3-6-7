\# SER516 Group 3

Team Members:

-Andrew

-Sarah

-Lily

-Mohamed

-Eric



\###Project Overview:

Group 3 has been tasked with creating services that in sum take a chunk of code and show the number of afferent and efferent couplings on the default branch.
Group 3 has also been tasked with creating services that in sum take a Taiga and show the number of instance of Cruft and On Time Delivery.

A/E Requirements: Default branch must either be named 'main' or 'master' and is case sensitive. The repository must be public on Github.com. You must know the github repository URL.
C/OTD Requirements: Taiga must be public, and you must know the Taiga URL. The Taiga must have at least 1 sprint with at least 1 user story, and at least 1 user story must be marked as 'done'.



\##Scope of services:

This will take a repository or taiga online, clone the repository or taiga onto a local machine storage location, iterate through the file system (ignoring nested classes) and keep track of each files Afferent and Efferent Couplings or Cruft and On Time Delivery.
This will run VIA Docker, and will be run directly through Gradle. The results will be outputted to the console, there is no support for Grafana.



\##Pre-requisites

-Docker installation

-Java 22

-A local or git based codebase and repository

-Grafana





\##To use our services


\###Running via Gradle directly 

Load the code base, or access it via your Terminal/CMD/Powershell. Both of our metrics are combined into one service, it can be run from the Afferent/Efferent service folder. Do not use the Taiga folder. First run a 'gradle clean', a 'gradle build', and then a 'gradle run' command. This will execute the service.

1. It will ask you to select a local or github repository, the only choice is Github, selecting local will ask you to choose github.
2. Enter the URL of the repository you want to analyze.
3. 3 The service will run, and output the results, on a per file basis, in the console. 
4. The program then terminates, but can be run indefinetly using step 1. It automatically overwrites previous checks and files every run.

\###Running via Docker

Load the code base, or access it via your Terminal/CMD/Powershell. Both of our metrics are combined into one service, it can be run from the Afferent/Efferent service folder. Do not use the Taiga folder. First run a 'gradle clean', a 'gradle build', and then a 'gradle run' command. This will execute the service.

1. Build the Docker image using the provided Dockerfile. This can be done with the command: `docker build -t your-image-name .`
3. You must have docker running on the grading machine
4. Run the service using docker run -it -p 8080:8080 my-service:latest
5. Select an option from the ones given using 1/2/3.
6. Enter the URL of the repository you want to analyze or Taiga board you want to analyze.
7. The service will run, and output the results, on a per file basis, in the console. 
4. The program then terminates, but can be run indefinetly using step 1. It automatically overwrites previous checks and files every run.





