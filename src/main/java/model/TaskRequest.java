package model;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.GeneratedMessageV3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskRequest {

    private final List<GeneratedMessageV3> taskRequestMessages = new ArrayList<>();

    private final boolean isNoArgRequest;

    private final String methodName;

    public TaskRequest(List<GeneratedMessageV3> taskRequestMessages, String methodName) {
        this.taskRequestMessages.addAll(taskRequestMessages);
        this.isNoArgRequest = false;
        this.methodName = methodName;
    }

    public TaskRequest(String methodName) {
        isNoArgRequest = true;
        this.methodName = methodName;
    }

    public List<byte[]> getSerializedMessages(){
        return taskRequestMessages.stream().map(AbstractMessageLite::toByteArray).collect(Collectors.toList());
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isNoArgRequest() {
        return isNoArgRequest;
    }
}
