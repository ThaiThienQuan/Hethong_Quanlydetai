package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Bảng {@code lecturers} - hồ sơ giảng viên, nối 1-1 với {@code users}. */
@Entity
@Table(name = "lecturers")
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "lecturer_code", nullable = false, unique = true, length = 20)
    private String lecturerCode;

    /** Học vị: Thạc sĩ, Tiến sĩ... */
    @Column(name = "academic_degree", length = 50)
    private String academicDegree;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    protected Lecturer() {
    }

    public Lecturer(User user, String lecturerCode, Department department) {
        this.user = user;
        this.lecturerCode = lecturerCode;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public void setLecturerCode(String lecturerCode) {
        this.lecturerCode = lecturerCode;
    }

    public String getAcademicDegree() {
        return academicDegree;
    }

    public void setAcademicDegree(String academicDegree) {
        this.academicDegree = academicDegree;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}