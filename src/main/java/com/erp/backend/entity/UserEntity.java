package com.erp.backend.entity;

import com.erp.backend.entity.Role;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    public UserEntity() {
    }

    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new HashSet<>();
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Role> getRoles() {
        if (roles == null) roles = new HashSet<>();
        return roles;
    }
    public void setRoles(Set<Role> roles) { this.roles = roles != null ? roles : new HashSet<>(); }

    // Helper Methods
    public void addRole(Role role) { getRoles().add(role); }
    public void removeRole(Role role) { getRoles().remove(role); }
    public boolean hasRole(Role role) { return getRoles().contains(role); }
}
