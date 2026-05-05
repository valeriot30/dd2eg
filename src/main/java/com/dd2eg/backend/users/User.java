package com.dd2eg.backend.users;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
public class User {

    @Id
    private String Id;

    @Indexed(unique = true)
    private String username;

    private String email;

    private String password;

    private UserType userType = UserType.DEVELOPER;
}
