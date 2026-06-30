package com.dd2eg.backend.model;


import com.dd2eg.backend.utils.UserType;
import com.dd2eg.backend.DTO.RecentProjectDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Document(collection = "users")
@Sharded(shardKey = { "_id" }, shardingStrategy = ShardingStrategy.HASH)
public class User implements UserDetails {

    @Id
    private String id;

    @TextIndexed
    private String username;

    @Indexed(unique = true)
    private String email;

    private String profilePic;

    private String password;

    private UserType userType = UserType.DEVELOPER;

    private DeveloperInfo developerInfo;

    private EnterpriseInfo enterpriseInfo;

    private List<Skill> skills = new ArrayList<>();

    private boolean enabled = true;

    @DBRef
    private List<Project> ownedProjects = new ArrayList<>();

    private List<RecentProjectDTO> lastProjects = new ArrayList<>();

    // TODO update when a commit is done
    private List<RecentProjectDTO> lastContributions = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userType.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
