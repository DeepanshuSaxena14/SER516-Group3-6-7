

import java.util.List;
import java.util.Scanner;


import org.taiga.CruftMetrics;
import org.taiga.DeliveryMetrics;
import org.json.JSONObject;
import org.taiga.TaigaClient;
import org.taiga.TaigaLoginObject;
import org.taiga.TaigaProject;public class main {






        int Afferent;
        int Efferent;


        //githubLoginObject GLO = new githubLoginObject();
        //public int choice = 1;

        public static void main(String[] args) {
            welcomeUser();

        }



        public static void welcomeUser(){
            Scanner scanner = new Scanner(System.in);
            System.out.println("Welcome to the Afferent Efferent Service!");
            System.out.println("Please select an option:");
            System.out.println("3. Connect to Taiga");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("Please use option 2");
                    break;
                case 2:
                    System.out.println("Please enter the GitHub repository URL:");
                    String repoUrl = scanner.next();
                    goClone(repoUrl);
                    break;
                case 3:
                    goTaiga(scanner);
                    break;
                default:
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }

        }

        public static void goClone(String s){
            CloneObject jgit = new CloneObject(s);
            String a = jgit.getRepoUrl();
            if(a.equals(s) && s.contains("github.com/")){
                try {
                    CloneObject.getRepoMetadata(s);
                    String localPath = CloneObject.cloneRepository(s);

                    List<Metrics> results =
                            Analyzer.analyze(localPath);


                    results.forEach(System.out::println);
                } catch (Exception e) {
                    System.out.println("Error cloning repository: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid repo URL");
            }

        }

        public static void goTaiga(Scanner scanner) {
            System.out.println("Enter your Taiga username:");
            String username = scanner.next();
            System.out.println("Enter your Taiga password:");
            String password = scanner.next();

            TaigaClient taiga = new TaigaClient();
            TaigaLoginObject loginObj = new TaigaLoginObject(username, password);

            try {
                boolean loggedIn = taiga.login(loginObj);
                if (!loggedIn) {
                    System.out.println("Could not log in to Taiga.");
                    return;
                }

                // if we get here login worked
                System.out.println("Logged in!");
                taiga.listProjects(loginObj);

                // let the user pick by id or slug since both are shown in the list
                System.out.println("Enter a project ID or slug:");
                String input = scanner.next();

                int projectId;
                try {
                    // if it parses as a number, treat it as an id
                    projectId = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    // otherwise look it up by slug
                    System.out.println("Looking up project by slug: " + input);
                    String projectBody = taiga.getProjectBySlug(loginObj, input);
                    if (projectBody == null) return;
                    projectId = new JSONObject(projectBody).getInt("id");
                }

                // get the full structure with sprints, user stories, and tasks all parsed out
                TaigaProject project = taiga.getStructure(loginObj, projectId);
                project.printStructure();

                System.out.println("Analyzing delivery metrics for the project...");
                try {
                    List<DeliveryMetrics> metrics = taiga.getDeliveryMetrics(loginObj, projectId);
                    for (DeliveryMetrics m : metrics) {
                        System.out.println(m);
                    }
                } catch (Exception e) {
                    System.out.println("Error analyzing delivery: " + e.getMessage());
                }

                // --- Cruft metric (user story #40 / tasks #54 & #55) ---
                // Cruft = % of stories representing zero-value (technical debt) work.
                // Shown per sprint so the caller can see how cruft evolves over time.
                System.out.println("\nAnalyzing cruft metrics for the project...");
                try {
                    List<CruftMetrics> cruftList = taiga.getCruftMetrics(loginObj, projectId);
                    if (cruftList.isEmpty()) {
                        System.out.println("No sprint data found for cruft analysis.");
                    } else {
                        double totalStories = 0;
                        double totalCruft = 0;
                        for (CruftMetrics c : cruftList) {
                            System.out.println(c);
                            totalStories += c.totalStories();
                            totalCruft += c.cruftStories();
                        }
                        double overallCruftPct = totalStories == 0 ? 0.0 : (totalCruft / totalStories * 100.0);
                        System.out.printf("%nOverall cruft across all sprints: %.1f%% (%d / %d stories)%n",
                                overallCruftPct, (int) totalCruft, (int) totalStories);
                    }
                } catch (Exception e) {
                    System.out.println("Error analyzing cruft: " + e.getMessage());
                }

            } catch (Exception e) {
                System.out.println("Error connecting to Taiga: " + e.getMessage());
            }
        }




    }


}
