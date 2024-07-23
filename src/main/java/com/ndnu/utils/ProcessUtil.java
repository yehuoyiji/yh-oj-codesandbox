package com.ndnu.utils;

import cn.hutool.core.util.StrUtil;
import com.ndnu.domian.ExecuteMessage;

import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtil {

    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName){
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(opName+"成功");
                //分批获取进程正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                //逐行读取
//                List<String> outputStrList = new ArrayList<>();
                String compileOutputLine;
                StringBuilder stringBuilder = new StringBuilder();
                while((compileOutputLine = bufferedReader.readLine()) != null){

                    stringBuilder.append(compileOutputLine);
                }
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                executeMessage.setMessage(stringBuilder.toString());
            } else {
                System.out.println(opName+"失败 " + exitValue);
                //分批获取进程正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                //逐行读取
                String compileOutputLine;
                StringBuilder stringBuilder = new StringBuilder();
                while((compileOutputLine = bufferedReader.readLine()) != null){
                    stringBuilder.append(compileOutputLine);
                }
                //分批获取进程erro输出
                BufferedReader erroBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                //逐行读取
                String erroCompileOutputLine;
                StringBuilder erroStringBuilder = new StringBuilder();
                while((erroCompileOutputLine = erroBufferedReader.readLine()) != null){
                    erroStringBuilder.append(erroCompileOutputLine);
                }
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                executeMessage.setErroMessage(erroStringBuilder.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String opName, String args){
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            InputStream inputStream = runProcess.getInputStream();
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            //相当于回车输入完成
            outputStreamWriter.flush();

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(opName+"成功");
                //分批获取进程正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                //逐行读取
//                List<String> outputStrList = new ArrayList<>();
                String compileOutputLine;
                StringBuilder stringBuilder = new StringBuilder();
                while((compileOutputLine = bufferedReader.readLine()) != null){
                    stringBuilder.append(compileOutputLine);
                }
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                executeMessage.setMessage(stringBuilder.toString());
                outputStreamWriter.close();
                outputStream.close();
                inputStream.close();
                runProcess.destroy();
            } else {
                System.out.println(opName+"失败 " + exitValue);
                //分批获取进程正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                //逐行读取
                String compileOutputLine;
                StringBuilder stringBuilder = new StringBuilder();
                while((compileOutputLine = bufferedReader.readLine()) != null){
                    stringBuilder.append(compileOutputLine);
                }
                //分批获取进程erro输出
                BufferedReader erroBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                //逐行读取
                String erroCompileOutputLine;
                StringBuilder erroStringBuilder = new StringBuilder();
                while((erroCompileOutputLine = erroBufferedReader.readLine()) != null){
                    erroStringBuilder.append(erroCompileOutputLine);
                }
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                executeMessage.setErroMessage(erroStringBuilder.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;

    }
}
