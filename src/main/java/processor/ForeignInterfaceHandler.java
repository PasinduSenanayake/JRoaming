package processor;

import concurrent.ThreadHandler;
import dto.Config;
import marker.ForeignService;
import marker.ForeignServiceImpl;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

public class ForeignInterfaceHandler {

    private static final ConcurrentHashMap<String, Object> invocationHandlersMap = new ConcurrentHashMap<>();

    private static final Config configuration = ConfigProcessor.generateConfig();

    private static final ThreadHandler threadHandler = new ThreadHandler();

    private static final Communicator communicator = new Communicator(threadHandler,configuration.isStandAloneServer());

    private static final ServiceHandler serviceHandler = new ServiceHandler(communicator, threadHandler, configuration);

    public static <T extends ForeignService> T wireForeignImpl(Class<T> interFaceClass, String serviceSignature, String implSignature) {

        invocationHandlersMap.putIfAbsent(serviceSignature + "_" + implSignature, Proxy.newProxyInstance(interFaceClass.getClassLoader(),
                new Class[]{interFaceClass},
                new ImplInvocationHandler(new OffLoader(communicator, serviceSignature, implSignature, configuration.getServiceAddress(serviceSignature)))));
        return (T) invocationHandlersMap.get(serviceSignature + "_" + implSignature);

    }

    public static void createForeignServiceImpl(String implSignature, ForeignServiceImpl foreignService) {
        serviceHandler.registerNewService(implSignature, foreignService);
    }
}
