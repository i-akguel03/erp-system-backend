package com.erp.backend.dto;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.UserEntity;

import java.util.Set;

public class UserAdminDto {

    private Long id;
    private String username;
    private Set<Role> roles;

    public UserAdminDto() {}

    public UserAdminDto(UserEntity user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.roles = user.getRoles();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public Set<Role> getRoles() { return roles; }
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
