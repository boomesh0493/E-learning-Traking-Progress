import java.sql.*;

public class ProgressTracker {

    // ---------- CREATE ----------
    public void addStudent(String name, String email) {
        String sql = "INSERT INTO students (name, email) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) System.out.println("Student added. ID = " + rs.getInt(1));
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("A student with that email already exists.");
        } catch (SQLException e) {
            printError(e);
        }
    }

    public void addCourse(String title, int totalLessons) {
        String sql = "INSERT INTO courses (title, total_lessons) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setInt(2, totalLessons);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) System.out.println("Course added. ID = " + rs.getInt(1));
            }
        } catch (SQLException e) {
            printError(e);
        }
    }

    public void enroll(int studentId, int courseId) {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
            System.out.println("Enrolled successfully.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Invalid student/course ID, or student is already enrolled.");
        } catch (SQLException e) {
            printError(e);
        }
    }

    // ---------- UPDATE ----------
    /** Sets the number of lessons completed and recalculates status in one transaction. */
    public void updateProgress(int studentId, int courseId, int lessonsCompleted) {
        String findSql = "SELECT c.total_lessons FROM enrollments e " +
                         "JOIN courses c ON c.course_id = e.course_id " +
                         "WHERE e.student_id = ? AND e.course_id = ?";
        String updSql  = "UPDATE enrollments SET lessons_completed = ?, status = ? " +
                         "WHERE student_id = ? AND course_id = ?";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement find = con.prepareStatement(findSql);
                 PreparedStatement upd  = con.prepareStatement(updSql)) {

                find.setInt(1, studentId);
                find.setInt(2, courseId);
                int total;
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("No such enrollment.");
                        con.rollback();
                        return;
                    }
                    total = rs.getInt(1);
                }
                if (lessonsCompleted < 0 || lessonsCompleted > total) {
                    System.out.println("Lessons completed must be between 0 and " + total + ".");
                    con.rollback();
                    return;
                }
                String status = lessonsCompleted == 0 ? "NOT_STARTED"
                              : lessonsCompleted == total ? "COMPLETED" : "IN_PROGRESS";

                upd.setInt(1, lessonsCompleted);
                upd.setString(2, status);
                upd.setInt(3, studentId);
                upd.setInt(4, courseId);
                upd.executeUpdate();
                con.commit();
                System.out.println("Progress updated. Status: " + status);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            printError(e);
        }
    }

    // ---------- READ ----------
    public void listCourses() {
        String sql = "SELECT course_id, title, total_lessons FROM courses ORDER BY course_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-5s %-35s %s%n", "ID", "Title", "Lessons");
            while (rs.next())
                System.out.printf("%-5d %-35s %d%n", rs.getInt(1), rs.getString(2), rs.getInt(3));
        } catch (SQLException e) {
            printError(e);
        }
    }

    public void listStudents() {
        String sql = "SELECT student_id, name, email FROM students ORDER BY student_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-5s %-25s %s%n", "ID", "Name", "Email");
            while (rs.next())
                System.out.printf("%-5d %-25s %s%n", rs.getInt(1), rs.getString(2), rs.getString(3));
        } catch (SQLException e) {
            printError(e);
        }
    }

    public void studentReport(int studentId) {
        String sql = "SELECT s.name, c.title, e.lessons_completed, c.total_lessons, " +
                     "ROUND(e.lessons_completed * 100.0 / c.total_lessons, 1) AS pct, e.status " +
                     "FROM enrollments e " +
                     "JOIN students s ON s.student_id = e.student_id " +
                     "JOIN courses  c ON c.course_id  = e.course_id " +
                     "WHERE e.student_id = ? ORDER BY c.title";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    if (!any) {
                        System.out.println("Progress report for: " + rs.getString("name"));
                        System.out.printf("%-32s %-10s %-8s %s%n", "Course", "Done", "Percent", "Status");
                        any = true;
                    }
                    System.out.printf("%-32s %-10s %-8s %s%n",
                            rs.getString("title"),
                            rs.getInt("lessons_completed") + "/" + rs.getInt("total_lessons"),
                            rs.getDouble("pct") + "%",
                            rs.getString("status"));
                }
                if (!any) System.out.println("No enrollments found for this student.");
            }
        } catch (SQLException e) {
            printError(e);
        }
    }

    public void courseSummary() {
        String sql = "SELECT c.title, COUNT(e.enrollment_id) AS enrolled, " +
                     "COALESCE(SUM(e.status = 'COMPLETED'), 0) AS completed, " +
                     "COALESCE(ROUND(AVG(e.lessons_completed * 100.0 / c.total_lessons), 1), 0) AS avg_pct " +
                     "FROM courses c LEFT JOIN enrollments e ON e.course_id = c.course_id " +
                     "GROUP BY c.course_id, c.title ORDER BY c.title";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-32s %-10s %-10s %s%n", "Course", "Enrolled", "Completed", "Avg %");
            while (rs.next())
                System.out.printf("%-32s %-10d %-10d %.1f%n",
                        rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4));
        } catch (SQLException e) {
            printError(e);
        }
    }

    // ---------- DELETE ----------
    public void deleteStudent(int studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            System.out.println(ps.executeUpdate() > 0
                    ? "Student (and their enrollments) deleted."
                    : "No student with that ID.");
        } catch (SQLException e) {
            printError(e);
        }
    }

    private void printError(SQLException e) {
        System.out.println("Database error: " + e.getMessage());
    }
}
