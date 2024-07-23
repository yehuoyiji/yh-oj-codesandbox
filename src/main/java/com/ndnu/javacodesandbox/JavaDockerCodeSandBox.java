package com.ndnu.javacodesandbox;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.ndnu.domian.ExecuteCodeRequest;
import com.ndnu.domian.ExecuteCodeResponse;
import com.ndnu.domian.ExecuteMessage;
import com.ndnu.domian.JudgeInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate {
    public static final Boolean FIRST_INIT = true;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public List<ExecuteMessage> runFile(List<String> inputList, File userCodeFile) {
        //3.创建容器，把文件复制到容器内
        //获取默认Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

//        //拉取镜像
        String image = "openjdk:8-alpine";
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
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
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
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
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
                    if (!StreamType.STDERR.equals(streamType)) {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + message[0]);
                    } else {
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
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = statistics.getMemoryStats().getMaxUsage();
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
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
        return executeMessageList;
    }

    /**
     * 重新得到返回值方法，将最大消耗内存填入返回值当中
     *
     * @param executeMessageList
     * @return
     */
    @Override
    public ExecuteCodeResponse getOutPutResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;//定义最大运行时间值；
        long maxMemory = 0;//定义最大内存值
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
            if (executeMessage.getMemory() != null) {
                maxMemory = Math.max(maxMemory, executeMessage.getMemory());
            }
        }

        //若正常运行无错误，判断
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);//1表示正常
        }
        executeCodeResponse.setOutput(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);//将最大消耗内存填入返回值当中

        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }
}
