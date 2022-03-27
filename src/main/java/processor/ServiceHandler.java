package processor;

import annotation.ForeignFunction;
import concurrent.ThreadHandler;
import dto.Config;
import dto.FunctionMetaData;
import dto.Pair;
import marker.ForeignServiceImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ServiceHandler {

    private final Communicator communicator;

    private ExecutorService executorService;

    private final Map<String, Map<String, FunctionMetaData>> foreignServiceMap = new ConcurrentHashMap<>();

    public ServiceHandler(Communicator communicator, ThreadHandler threadHandler, Config configuration) {
        this.communicator = communicator;
        if (!configuration.getBindAddresses().isEmpty()) {
            this.executorService = threadHandler.initiateServiceThreadPool(configuration.getServiceParallelism());
            this.communicator.registerReceiver(configuration.getBindAddresses(), this::listen);
        }

    }

    public void registerNewService(String implSignature, ForeignServiceImpl foreignService) {

        try {
            ConcurrentHashMap<String, FunctionMetaData> functionMetaMap = new ConcurrentHashMap<>();

            Class<?> classOfImpl = foreignService.getClass();

            for (Method method : classOfImpl.getMethods()) {

                if (!method.isAnnotationPresent(ForeignFunction.class)) {
                    continue;
                }

                ForeignFunction foreignFunction = method.getAnnotation(ForeignFunction.class);

                FunctionMetaData functionMetaData = new FunctionMetaData(foreignFunction.functionSignature(), foreignService, method);

                List<Method> inputParsers = new ArrayList<>();

                for (Parameter parameter : method.getParameters()) {
                    Class<?> typedClass = parameter.getType();
                    Method parseFrom = typedClass.getMethod("parseFrom", byte[].class);
                    inputParsers.add(parseFrom);
                }

                functionMetaData.addInputParamParses(inputParsers);

                if (!method.getReturnType().equals(Void.TYPE)) {
                    Class<?> returnType;
                    boolean isAsync;
                    if ("java.util.concurrent.Future".equals(method.getReturnType().getTypeName())) {
                        String genericReturnType = method.getGenericReturnType().getTypeName();
                        String className = genericReturnType.split("<")[1].split(">")[0];
                        returnType = Class.forName(className);
                        isAsync = true;
                    } else {
                        returnType = method.getReturnType();
                        isAsync = false;
                    }
                    Method convertToByteArray = returnType.getMethod("toByteArray");
                    functionMetaData.setReturnParser(convertToByteArray,isAsync);
                }

                functionMetaMap.put(foreignFunction.functionSignature(), functionMetaData);

            }

            foreignServiceMap.put(implSignature, functionMetaMap);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void listen(Pair<String, List<byte[]>> request) {

        executorService.submit(() -> {
            try {

                String[] methodSignatureData = request.getFirstElement().split("::");
                List<byte[]> requestArgs = request.getSecondElement();

                FunctionMetaData functionMetaData = foreignServiceMap.get(methodSignatureData[0]).get(methodSignatureData[1]);

                List<Object> inputParams = new ArrayList<>();

                List<Method> parseFromMethodList = functionMetaData.getParamParseList();

                for (int i = 0; i < requestArgs.size(); i++) {
                    inputParams.add(parseFromMethodList.get(i).invoke(null, requestArgs.get(i)));
                }

                Object obj = functionMetaData.getFunction().invoke(functionMetaData.getForeignService(), inputParams.toArray(new Object[0]));

                if (functionMetaData.isVoid()) {
                    communicator.sendReply(request.getFirstElement());
                } else {
                    Object rawObject;
                   if (functionMetaData.isReturnAsync()){
                       Future rawObjectFuture = (Future) obj;
                       rawObject = rawObjectFuture.get();
                   }else {
                       rawObject = obj;
                   }
                    communicator.sendReply(request.getFirstElement(), (byte[]) functionMetaData.getReturnParser().invoke(rawObject));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

}
