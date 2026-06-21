package com.dd2eg.backend.DTO;

import com.dd2eg.backend.utils.UserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDTO {

    private String username;
    private String email;
    private String password;

    private UserType userType;
}