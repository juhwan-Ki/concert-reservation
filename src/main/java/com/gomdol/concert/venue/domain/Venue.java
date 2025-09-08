package com.gomdol.concert.venue.domain;


import lombok.Getter;

@Getter
public class Venue {
    private final Long id;
    private final String name;
    private final String address;
    private final int capacity;

    private Venue(Long id, String name, String address, int capacity) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.capacity = capacity;
    }

    public static Venue create(Long id, String name, String address, int capacity) {
        return new Venue(id, name, address, capacity);
    }
}
