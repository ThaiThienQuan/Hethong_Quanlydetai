package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

/** Bảng {@code roles}. Vai trò gốc của hệ thống được đánh dấu {@code isSystem = true}. */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ADMIN, TRUONG_KHOA, GIANG_VIEN, SINH_VIEN hoặc vai trò tự tạo. */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /** 1 = vai trò gốc, không được xóa. Tên field không có tiền tố "is" để Spring Data
     *  suy ra property đúng (getter isSystem() -> property "system"). */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    /** Bảng nối {@code role_permissions} - được chỉnh trên màn hình Phân quyền. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @jakarta.persistence.OrderBy("code asc")
    private Set<Permission> permissions = new LinkedHashSet<>();

    protected Role() {
    }

    public Role(String code, String name, boolean system) {
        this.code = code;
        this.name = name;
        this.system = system;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}