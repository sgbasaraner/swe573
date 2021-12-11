package io.github.sgbasaraner.funxchange.model;

public class AuthRequest {
    private final String userName;
    private final String password;

    public AuthRequest(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
