package processor;

import annotation.ForeignFunction;
import com.google.protobuf.GeneratedMessageV3;
import dto.AsyncFuture;
import model.TaskRequest;
import model.TaskResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class ImplInvocationHandler implements InvocationHandler {

    private final OffLoader offLoader;

    public ImplInvocationHandler(OffLoader offLoader) {
        this.offLoader = offLoader;
    }

    public Object invoke(Object proxy, Method method, Object[] args) {

        try {

            List<GeneratedMessageV3> generatedMessageV3List = new ArrayList<>();
            for (Object arg : args) {
                generatedMessageV3List.add((GeneratedMessageV3) arg);
            }

            TaskRequest taskRequest = new TaskRequest(generatedMessageV3List, method.getAnnotation(ForeignFunction.class).functionSignature());
            Future<TaskResponse> taskResponseFuture = offLoader.offloadTask(taskRequest);
            if ("java.util.concurrent.Future".equals(method.getReturnType().getTypeName())) {
                String genericReturnType = method.getGenericReturnType().getTypeName();
                String className =  genericReturnType.split("<")[1].split(">")[0];
                Class<?> typedClass = Class.forName(className);
                Method parseFrom = typedClass.getMethod("parseFrom", byte[].class);
                return new AsyncFuture(taskResponseFuture, parseFrom, typedClass);
            } else {
                Class<?> returnTypeClass = method.getReturnType();
                if(returnTypeClass.getTypeName().equals("void")){
                    taskResponseFuture.get();
                    return null;
                }
                Method parseFrom = returnTypeClass.getMethod("parseFrom", byte[].class);
                Object obj = parseFrom.invoke(null, taskResponseFuture.get().getTaskResponseMessage());
                Class<?> typedClass = Class.forName(returnTypeClass.getName());
                return typedClass.cast(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }
}

