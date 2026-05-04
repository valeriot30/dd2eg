package com.dd2eg.backend.users;


import org.springframework.data.annotation.Id;

public class User {

    @Id
    public String Id;

    public String username;

    public String email;

    public String password;
}
