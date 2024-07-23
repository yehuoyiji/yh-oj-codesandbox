package com.ndnu;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ndnu.ccodesandbox.CNationCodeSandBoxOld;
import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;
import com.ndnu.domian.ExecuteMessage;
import com.ndnu.domian.JudgeInfo;
import com.ndnu.javacodesandbox.JavaDockerCodeSandBoxOld;
import com.ndnu.javacodesandbox.JavaNationCodeSandBox;
import com.ndnu.javacodesandbox.JavaNationCodeSandBoxOld;
import com.ndnu.utils.ProcessUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpringBootTest
class NdnuCodeSandboxApplicationTests {

    @Resource
    private JavaDockerCodeSandBoxOld javaDockerCodeSandBoxOld;
    @Resource
    private CNationCodeSandBoxOld cNationCodeSandBoxOld;
    @Resource
    private JavaNationCodeSandBox javaNationCodeSandBox;


    @Test
    void testCSandBox(){
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
//        String code =
//                "#include <stdio.h>  \n" +
//                        "  \n" +
//                        "int main() {  \n" +
//                        "    char str[100];  \n" +
//                        "    printf(\"请输入一个字符串: \");  \n" +
//                        "    scanf(\"%s\", str); // 注意，%s不会读取空格，只会读取到第一个空格为止  \n" +
//                        "    printf(\"你输入的字符串是: %s\\n\", str);  \n" +
//                        "    return 0;  \n" +
//                        "}";
        String code = "#include <stdio.h>  \n" +
                "#include <stdbool.h> // 引入stdbool.h头文件以使用bool类型  \n" +
                "  \n" +
                "// 声明isPalindrome函数，使其在main函数之前可见  \n" +
                "bool isPalindrome(int x);  \n" +
                "  \n" +
                "int main() {\n" +
                "    int num;\n" +
                "\tprintf(\"请输入一个整数: \");  \n" +
                "    scanf(\"%d\", &num);    \n" +
                "    bool result = isPalindrome(num); // 调用isPalindrome函数并存储返回值  \n" +
                "    if (result) {  \n" +
                "        printf(\"%d 是回文数\\n\", num);  \n" +
                "    } else {  \n" +
                "        printf(\"%d 不是回文数\\n\", num);  \n" +
                "    }  \n" +
                "    return 0;  \n" +
                "}  \n" +
                "  \n" +
                "// 定义isPalindrome函数  \n" +
                "bool isPalindrome(int x) {  \n" +
                "    if (x < 0) {  \n" +
                "        return false;  \n" +
                "    }  \n" +
                "    long int sum = 0;  \n" +
                "    long int n = x;  \n" +
                "    while (n != 0) {  \n" +
                "        sum = sum * 10 + n % 10;  \n" +
                "        n = n / 10;  \n" +
                "    }  \n" +
                "    if (sum == x) {  \n" +
                "        return true;  \n" +
                "    } else {  \n" +
                "        return false;  \n" +
                "    }  \n" +
                "}";
        List<String> inputList = Arrays.asList("121");
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("c");
        executeCodeRequest.setInputList(inputList);
        ExecuteCodeResponse executeCodeResponse = cNationCodeSandBoxOld.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
    @Test
    void testJavaSandBox(){
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = "import java.util.Scanner;\n" +
                "\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Scanner sc = new Scanner(System.in);\n" +
                "        int a = sc.nextInt();\n" +
                "        int b = sc.nextInt();\n" +
                "        System.out.println(a + b);\n" +
                "    }\n" +
                "}";
        List<String> inputList = Arrays.asList("1 2");
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("c");
        executeCodeRequest.setInputList(inputList);
        ExecuteCodeResponse executeCodeResponse = javaNationCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Test
    void contextLoads() {
        //1.找到根目录，在根目录下打开文件夹（判断 没有就创建）
        String userDir = System.getProperty("user.dir");
        //创建文件
        String globalCodePathName = userDir + File.separator + "tmpCode";
        String code = "public class Main { public static void main(String[] args) {int a = Integer.parseInt(args[0]);int b = Integer.parseInt(args[1]);System.out.println(\"成功：\"+(a+b));} }";
        List<String> inputList = Arrays.asList("1 2", "1 3");

        //判断
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //2.隔离用户的代码在新的文件夹下
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID().toString();
        String userCodePath = userCodeParentPath + File.separator + "Main.java";
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        //2.编译代码生成class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compilerProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compilerProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //2.运行代码生成class文件
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputList) {
            String runCmd = String.format("java -Dfile.encoding=utf-8 -cp %s Main %s", userCodeParentPath, input);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //4.收集整理信息
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;//定义最大运行时间值；
        for (ExecuteMessage executeMessage : executeMessageList) {
            String erroMessage = executeMessage.getErroMessage();
            if (StrUtil.isNotBlank(erroMessage)) {
                executeCodeResponse.setMessage(erroMessage);
                executeCodeResponse.setStatus(3);//3表示执行错误；
                break;
            }
            outputList.add(erroMessage);
            if (executeMessage.getTime() != null) {
                maxTime = Math.max(maxTime, executeMessage.getTime());
            }
        }

        //若正常运行无错误，判断
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);//1表示正常
        }
        executeCodeResponse.setOutput(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
//        judgeInfo.setMemory();

        executeCodeResponse.setJudgeInfo(judgeInfo);
        //5.删除文件
        if(userCodeFile.getParentFile() != null){
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
    }
}



