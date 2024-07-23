package com.ndnu.javacodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ndnu.CodeSandbox;
import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;
import com.ndnu.domian.ExecuteMessage;
import com.ndnu.domian.JudgeInfo;
import com.ndnu.utils.ProcessUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * java代码沙箱模板方法
 */
public abstract class JavaCodeSandBoxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    public static final Long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //1.把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //2.编译代码生成class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        //3.运行代码返回结果
        List<ExecuteMessage> executeMessageList = runFile(inputList, userCodeFile);

        //4.收集整理信息
        ExecuteCodeResponse outPutResponse = getOutPutResponse(executeMessageList);

        //5.删除文件
        deleteFile(userCodeFile);
        return outPutResponse;
    }


    /**
     * 1.把用户代码保存为文件
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
        //1.找到根目录，在根目录下打开文件夹（判断 没有就创建）
        String userDir = System.getProperty("user.dir");
        //创建文件
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //隔离用户的代码在新的文件夹下
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID().toString();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译代码生成class文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            //3.执行程序
            Process compilerProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compilerProcess, "编译");
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException("编译异常", e);
        }

    }

    /**
     * 3.运行代码返回结果
     * @param inputList
     * @param userCodeFile
     * @return
     */
    public List<ExecuteMessage>  runFile(List<String> inputList, File userCodeFile){
        File userCodeParentPath = userCodeFile.getParentFile().getAbsoluteFile();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputList) {
            //-Xmx512M指定最堆空间为512M
            String runCmd = String.format("java -Xmx512M -Dfile.encoding=utf-8 -cp %s Main %s", userCodeParentPath, input);
            try {
                //3.执行程序
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runInteractProcessAndGetMessage(runProcess, "运行", input);
                System.out.println(executeMessage);
                if(executeMessage.getExitValue() == null){
                    executeMessage.setErroMessage("编译异常");
                }
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("程序异常", e);
            }
        }
        return executeMessageList;
    }

    /**
     *  4.收集整理信息结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutPutResponse(List<ExecuteMessage> executeMessageList){
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
            outputList.add(executeMessage.getMessage());
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
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5.删除文件
     * @param userCodeFile
     * @return
     */
    public Boolean deleteFile(File userCodeFile){
        userCodeFile.deleteOnExit();
        return true;
    }

    /**
     * 6.错误处理
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutput(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);//表示代码沙箱错误，非运行错误
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
