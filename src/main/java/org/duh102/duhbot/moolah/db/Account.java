package org.duh102.duhbot.moolah.db;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table( name = "bankAccount" )
public class Account {
    @Column(name="uid", nullable=false)
    private Long uid;

    @Column(name="user", nullable=false, unique=true)
    private String username;

    @Column(name="balance", nullable=false)
    private BigInteger balance = BigInteger.ZERO;

    @Column(name="lastMined", nullable=false)
    private Instant lastMined = Instant.now().minus(2, ChronoUnit.DAYS);

    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public Long getUid() {
        return uid;
    }
    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public BigInteger getBalance() {
        return balance;
    }
    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public Instant getLastMined() {
        return lastMined;
    }
    public void setLastMined(Instant lastMined) {
        this.lastMined = lastMined;
    }
}
