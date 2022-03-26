package processor;

import annotation.ForeignFunction;
import concurrent.ThreadHandler;
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

public class ServiceHandler {

    private final Communicator communicator;

    private ExecutorService executorService;

    private final Map<String, Map<String, FunctionMetaData>> foreignServiceMap = new ConcurrentHashMap<>();


    public ServiceHandler(Communicator communicator, ThreadHandler threadHandler, boolean isRequired) {
        this.communicator = communicator;
        if(isRequired){
            this.executorService = threadHandler.initiateServiceThreadPool(1);
            this.communicator.registerReceiver("sdsdsdsds",this::listen);
        }

    }

    public void registerNewService(String implSignature, ForeignServiceImpl foreignService){

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
                    Class<?> typedClass = Class.forName(parameter.getName());
                    Method parseFrom = typedClass.getMethod("parseFrom", byte[].class);
                    inputParsers.add(parseFrom);
                }

                functionMetaData.addInputParamParses(inputParsers);

                if (!method.getReturnType().equals(Void.TYPE)) {
                    Class<?> returnType = method.getReturnType();
                    Method convertToByteArray = returnType.getMethod("toByteArray");
                    functionMetaData.setReturnParser(convertToByteArray);
                }

                functionMetaMap.put(foreignFunction.functionSignature(), functionMetaData);

            }

            foreignServiceMap.put(implSignature, functionMetaMap);
        } catch (Exception e){
            e.printStackTrace();
        }

    }


    private void listen(Pair<String, List<byte[]>> request) {

        executorService.submit(()->{
            try{

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
                    communicator.sendReply(request.getFirstElement(),(byte[])functionMetaData.getReturnParser().invoke(obj));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });



    }


}
