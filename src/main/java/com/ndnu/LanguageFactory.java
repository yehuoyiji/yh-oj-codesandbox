package com.ndnu;


import com.ndnu.ccodesandbox.CNationCodeSandBoxOld;
import com.ndnu.javacodesandbox.JavaNationCodeSandBox;
import com.ndnu.javacodesandbox.JavaNationCodeSandBoxOld;

/**
 * 代码沙箱工厂，可直接输入字符串调用
 */
public class LanguageFactory {
    public static CodeSandbox newInstance(String language) {
        switch (language){
            case "java":
                return new JavaNationCodeSandBox();
            case "c":
                return new CNationCodeSandBoxOld();
            case "python":
                return new JavaNationCodeSandBox();
            default:
                return new JavaNationCodeSandBox();
        }
    }
}
