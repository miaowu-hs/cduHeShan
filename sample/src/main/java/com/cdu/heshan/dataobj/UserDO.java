package com.cdu.heshan.dataobj;

import com.cdu.heshan.model.User;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.LocalDateTime;


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class UserDO implements Serializable {

    private long id;

    private String userName;

    private String pwd;

    private String nickName;

    private String avatar;

    private LocalDateTime gmtCreated;

    private LocalDateTime gmtModified;

    public UserDO() {
    }

    public UserDO(long id, String userName, String pwd, String nickName, String avatar, LocalDateTime gmtCreated, LocalDateTime gmtModified) {
        this.id = id;
        this.userName = userName;
        this.pwd = pwd;
        this.nickName = nickName;
        this.avatar = avatar;
        this.gmtCreated = gmtCreated;
        this.gmtModified = gmtModified;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(LocalDateTime gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    /**
     * DO 转换为 Model
     *
     * @return
     */
    public User toModel() {
        User user = new User();
        user.setId(getId());
        user.setUserName(getUserName());
        user.setNickName(getNickName());
        user.setAvatar(getAvatar());
        user.setGmtCreated(getGmtCreated());
        user.setGmtModified(getGmtModified());
        return user;
    }
}