package top.ywj.zhiji2026.data.remote.dto;

import java.util.List;
import top.ywj.zhiji2026.data.model.Course;

public class CourseListResponse {
    private List<Course> courses;

    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }
}
