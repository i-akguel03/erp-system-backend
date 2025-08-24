package com.erp.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class UserEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    // ✅ WICHTIG: Set direkt initialisieren
    private Set<Role> roles = new HashSet<>();

    // Konstruktoren
    public UserEntity() {
        // Default Constructor - roles ist bereits initialisiert
    }

    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new HashSet<>(); // Sicherheitshalber auch hier
    }

    // Getter und Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        // ✅ Zusätzliche Sicherheit: Lazy Initialization
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    // Helper Methods
    public void addRole(Role role) {
        getRoles().add(role); // Nutzt die sichere getRoles() Methode
    }

    public void removeRole(Role role) {
        getRoles().remove(role);
    }

    public boolean hasRole(Role role) {
        return getRoles().contains(role);
    }
}