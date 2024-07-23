package com.ndnu.domian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 输出用例
     */
    private List<String> output;

    /**
     * 信息
     */
    private String message;

    /**
     * 状态码
     */
    private Integer status;

    /**
     * 判题参数
     */
    private JudgeInfo judgeInfo;
}
