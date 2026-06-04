package com.dd2eg.backend.auth.dto;

import com.dd2eg.backend.users.UserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    private String username;
    private String email;
    private String password;

    private UserType userType;
}