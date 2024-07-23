package com.ndnu.javacodesandbox;

import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * java原生代码沙箱模板方法实现
 */
@Component
public class JavaNationCodeSandBox extends JavaCodeSandBoxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
