package com.ndnu.domian;

import lombok.Data;



@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String erroMessage;

    private Long time;

    private Long memory;
}
