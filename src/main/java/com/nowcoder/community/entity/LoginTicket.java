package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class LoginTicket {

    private int id;
    private int userId;
    private int status;
    private String ticket;
    private Date expired;

    public LoginTicket(int userId, int status, String ticket, Date expired) {
        this.userId = userId;
        this.status = status;
        this.ticket = ticket;
        this.expired = expired;
    }
}
