package com.dd2eg.backend.model;

import com.dd2eg.backend.utils.UserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectScamReport {

    private String reportingUserId;

    private String reportingUsername;

    private String reportingUserProfilePic;

    private UserType reportingUserType;

    private String comment;
}
