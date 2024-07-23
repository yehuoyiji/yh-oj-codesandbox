package com.ndnu.controller;

import com.ndnu.CodeSandbox;
import com.ndnu.LanguageFactory;
import com.ndnu.ccodesandbox.CNationCodeSandBoxOld;
import com.ndnu.javacodesandbox.JavaNationCodeSandBox;
import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class Main {

    @Resource
    private CNationCodeSandBoxOld cNationCodeSandBoxOld;

    @Resource
    private JavaNationCodeSandBox javaNationCodeSandBox;
    public static final String AUTH_REQUEST_HEADER = "auth";
    public static final String AUTH_REQUEST_SECRET = "secretKey";

    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request
                                    , HttpServletResponse response){
        String header = request.getHeader(AUTH_REQUEST_HEADER);
        if(!header.equals(AUTH_REQUEST_SECRET)){
            response.setStatus(403);
            return null;
        }
        if(executeCodeRequest == null){
            throw new RuntimeException("请求参数为空");
        }
        String language = executeCodeRequest.getLanguage();
        CodeSandbox codeSandbox = LanguageFactory.newInstance(language);
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        return executeCodeResponse;
    }
}
