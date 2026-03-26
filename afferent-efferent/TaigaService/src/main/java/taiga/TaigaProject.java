package org.taiga;

import java.util.ArrayList;
import java.util.List;

// holds all the parsed taiga data in a structured format
// structure: project -> sprints -> user stories -> tasks
public class TaigaProject {

    public String projectName;
    public int projectId;
    public String createdDate;
    public List<Sprint> sprints = new ArrayList<>();

    // sprint = milestone in taiga's API
    public static class Sprint {
        public String name;
        public int id;
        public String startDate;
        public String finishDate;
        public List<UserStory> userStories = new ArrayList<>();
    }

    public static class UserStory {
        public String name;
        public int id;
        public String createdDate;
        public List<Task> tasks = new ArrayList<>();
    }

    public static class Task {
        public int id;
        public String name;
        public String createdDate;
    }

    // prints the project structure over time, organized by sprint
    public void printStructure() {
        System.out.println("Project: " + projectName + " (created: " + createdDate + ")");
        for (Sprint sprint : sprints) {
            System.out.println("- Sprint: " + sprint.name +
                    " (" + sprint.startDate + " to " + sprint.finishDate + ")");
            for (UserStory us : sprint.userStories) {
                System.out.println("  - User Story: " + us.name +
                        " (id: " + us.id + ", created: " + us.createdDate + ")");
                for (Task task : us.tasks) {
                    System.out.println("    - Task: " + task.name +
                            " (id: " + task.id + ", created: " + task.createdDate + ")");
                }
            }
        }
    }
}
