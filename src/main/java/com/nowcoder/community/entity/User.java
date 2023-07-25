package com.nowcoder.community.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nowcoder.community.util.CommunityConstant;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
/*可以在User对象在序列化时忽略这些字段*/
@JsonIgnoreProperties({"enabled","accountNonExpired", "accountNonLocked", "credentialsNonExpired", "authorities"})
public class User implements UserDetails, CommunityConstant {

    private int id;
    private String username;
    private String password;
    private String salt;
    private String email;
    private int type;
    private int status;
    private String activationCode;
    private String headerUrl;
    private Date createTime;

    /*true:账号未过期*/
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /*true:账号未锁定*/
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /*true:凭证未过期*/
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /*true:账号可用*/
    @Override
    public boolean isEnabled() {
        return true;
    }


    /*返回用户具有的权限*/
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (type){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
