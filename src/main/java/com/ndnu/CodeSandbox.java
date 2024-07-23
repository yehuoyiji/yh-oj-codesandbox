package com.ndnu;


import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;

public interface CodeSandbox {

    /**
     * 执行代码模块
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
