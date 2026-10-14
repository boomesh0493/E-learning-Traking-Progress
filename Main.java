import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        ProgressTracker tracker = new ProgressTracker();
        while (true) {
            System.out.println("\n===== E-Learning Progress Tracker =====");
            System.out.println("1. Add student");
            System.out.println("2. Add course");
            System.out.println("3. Enroll student in course");
            System.out.println("4. Update lessons completed");
            System.out.println("5. View student progress report");
            System.out.println("6. View course summary");
            System.out.println("7. List students");
            System.out.println("8. List courses");
            System.out.println("9. Delete student");
            System.out.println("0. Exit");
            int choice = readInt("Choose: ");

            switch (choice) {
                case 1 -> tracker.addStudent(readLine("Name: "), readLine("Email: "));
                case 2 -> tracker.addCourse(readLine("Course title: "), readInt("Total lessons: "));
                case 3 -> tracker.enroll(readInt("Student ID: "), readInt("Course ID: "));
                case 4 -> tracker.updateProgress(readInt("Student ID: "), readInt("Course ID: "),
                                                 readInt("Lessons completed: "));
                case 5 -> tracker.studentReport(readInt("Student ID: "));
                case 6 -> tracker.courseSummary();
                case 7 -> tracker.listStudents();
                case 8 -> tracker.listCourses();
                case 9 -> tracker.deleteStudent(readInt("Student ID: "));
                case 0 -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
