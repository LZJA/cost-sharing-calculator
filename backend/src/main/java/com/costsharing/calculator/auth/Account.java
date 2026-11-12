package com.costsharing.calculator.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
@Data @Entity @Table(name="app_accounts")
public class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @Column(nullable=false, length=128) private String openid;
    // OpenID 区分大小写，用摘要索引避免 MySQL 默认字符排序规则合并不同身份。
    @JsonIgnore @Column(nullable=false, unique=true, length=64) private String openidHash;
    @Column(nullable=false, length=40) private String displayName = "微信用户";
}
