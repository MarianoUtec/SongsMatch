package com.musicmatch.event;

import com.musicmatch.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

// UserRegisteredEvent.java
@Getter
public class UserRegisteredEvent extends ApplicationEvent {
    private final User user;
    public UserRegisteredEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
