package processor;

import concurrent.ThreadHandler;
import marker.ForeignService;
import marker.ForeignServiceImpl;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

public class ForeignInterfaceHandler {

    private static final ConcurrentHashMap<String, Object> invocationHandlersMap = new ConcurrentHashMap<>();

    private static final ThreadHandler threadHandler = new ThreadHandler();

    private static final Communicator communicator = new Communicator(threadHandler);

    private static final ServiceHandler serviceHandler = new ServiceHandler(communicator, threadHandler, true);


    public static <T extends ForeignService> T wireForeignImpl(Class<T> interFaceClass, String entityName, String implSignature) {

        invocationHandlersMap.putIfAbsent(entityName + "_" + implSignature, Proxy.newProxyInstance(interFaceClass.getClassLoader(),
                new Class[]{interFaceClass},
                new ImplInvocationHandler(new OffLoader(communicator, entityName, implSignature, "sdsdssd"))));

        return (T) invocationHandlersMap.get(interFaceClass);

    }

    public static void createForeignServiceImpl(String implSignature, ForeignServiceImpl foreignService) {
        serviceHandler.registerNewService(implSignature, foreignService);
    }
}
