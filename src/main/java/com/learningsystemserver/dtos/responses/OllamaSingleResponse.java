package com.learningsystemserver.dtos.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaSingleResponse {

    @JsonProperty("model")
    private String model;

    @JsonProperty("response")
    private String response;

    @JsonProperty("done")
    private Boolean done;

}