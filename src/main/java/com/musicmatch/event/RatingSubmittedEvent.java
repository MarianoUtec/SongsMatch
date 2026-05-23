package com.musicmatch.event;

import com.musicmatch.entity.Rating;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RatingSubmittedEvent extends ApplicationEvent {
    private final Rating rating;
    private final Long userId;

    public RatingSubmittedEvent(Object source, Rating rating) {
        super(source);
        this.rating = rating;
        this.userId = rating.getUser().getId();
    }
}
