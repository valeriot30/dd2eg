package com.dd2eg.backend.DTO;

import com.dd2eg.backend.utils.UserType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SignupRequestDTO {

    private String username;
    private String email;
    private String password;

    private String profilePic;

    private List<String> skills;

    private String company_name;

    private String company_website;

    private UserType role;
}