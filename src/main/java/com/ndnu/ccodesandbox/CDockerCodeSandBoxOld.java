package com.ndnu.ccodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.ndnu.CodeSandbox;
import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;
import com.ndnu.domian.ExecuteMessage;
import com.ndnu.domian.JudgeInfo;
import com.ndnu.utils.ProcessUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CDockerCodeSandBoxOld implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.c";
    public static final Long TIME_OUT = 5000L;

    public static final Boolean FIRST_INIT = true;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        //1.找到根目录，在根目录下打开文件夹（判断 没有就创建）
        String userDir = System.getProperty("user.dir");
        //创建文件
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //2.隔离用户的代码在新的文件夹下
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID().toString();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        String compileCodePath = userCodeParentPath + File.separator + "Main.exe";
        //2.编译代码生成class文件
        String compileCmd = String.format("gcc -o %s  %s",compileCodePath,userCodePath);
//        String compileCmd = String.format("gcc -o ", userCodeFile.getAbsolutePath());
        try {
            //3.执行程序
            Process compilerProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compilerProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }


        //3.创建容器，把文件复制到容器内
        //获取默认Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        //拉取镜像
        String image = "gcc";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        //挂载绑定在根目录下新建一个app文件夹(尽量不在根目录下同步挂载）
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withCpuCount(1L);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();

        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        //启动容器
        dockerClient.startContainerCmd(containerId).exec();

        //先createExec再startExec
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = input.split(" ");
            long time = 0L;
            //以数组的方式拼接执行的命令（hutool包下的ArrayUtil.append可如下拼接数组）
            String[] cmdArray = ArrayUtil.append(new String[]{"/app/Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令成功" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            String createCmdResponseId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if(!StreamType.STDERR.equals(streamType)){
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + message[0]);
                    }else{
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + errorMessage[0]);
                    }
                    super.onNext(frame);
                }
            };

            //获取占用的内存
            final long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = statistics.getMemoryStats().getMaxUsage();
                }
                @Override
                public void onStart(Closeable closeable) {}
                @Override
                public void onError(Throwable throwable) {}
                @Override
                public void onComplete() {}
                @Override
                public void close() throws IOException {}
            });
            try {
                stopWatch.start();
                dockerClient.execStartCmd(createCmdResponseId)
                        .exec(execStartResultCallback)
                        .awaitCompletion();//异步操作,创建异步方法
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErroMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;//定义最大运行时间值；
        long maxMemory = 0;//定义最大内存值
        for (ExecuteMessage executeMessage : executeMessageList) {
            String erroMessage = executeMessage.getErroMessage();
            if(StrUtil.isNotBlank(erroMessage)){
                executeCodeResponse.setMessage(erroMessage);
                executeCodeResponse.setStatus(3);//3表示执行错误；
                break;
            }
            outputList.add(erroMessage);
            if(executeMessage.getTime() != null){
                maxTime = Math.max(maxTime,executeMessage.getTime());
            }
            if(executeMessage.getMemory() != null){
                maxMemory = Math.max(maxMemory,executeMessage.getMemory());
            }
        }

        //若正常运行无错误，判断
        if(outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);//1表示正常
        }
        executeCodeResponse.setOutput(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);

        executeCodeResponse.setJudgeInfo(judgeInfo);

        //5.删除文件
        userCodeFile.deleteOnExit();
        return executeCodeResponse;
    }

    //6.错误处理
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutput(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);//表示代码沙箱错误，非运行错误
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
