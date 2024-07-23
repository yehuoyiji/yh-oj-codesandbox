package com.ndnu.domian;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {


    /**
     * 程序执行信息
     */
    private String message;


    /**
     * 消耗的时间(ms)
     */
    private Long time;

    /**
     *  消耗的内存(kb)
     */
    private Long memory;



}
