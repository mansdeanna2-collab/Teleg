package org.telegram.admin.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String email;

    public LoginResponse(String token, String username, String role, String email) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
}
