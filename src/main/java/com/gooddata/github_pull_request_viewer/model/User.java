package com.gooddata.github_pull_request_viewer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private final String username;

    @JsonCreator
    public User(@JsonProperty("login") final String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
