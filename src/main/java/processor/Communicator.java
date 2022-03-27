package processor;

import concurrent.AsyncFunction;
import util.Listener;
import concurrent.ThreadHandler;
import dto.Pair;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static util.Constant.COMMUNICATOR_DELAY;

public class Communicator {

    private final Map<String, Listener<Optional<byte[]>>> replyListenerMap = new ConcurrentHashMap<>();

    private final Queue<Pair<String, Optional<byte[]>>> completedTaskQueue = new ConcurrentLinkedQueue<>();

    private final Map<String, Map<String, List<byte[]>>> sentTaskMap = new HashMap<>();

    private final Map<String, byte[]> pendingTaskMap = new HashMap<>();

    private final Queue<Pair<String, Pair<String, List<byte[]>>>> concurrentTaskQueue = new ConcurrentLinkedQueue<>();

    private final Map<String, ZMQ.Socket> socketMap = new HashMap<>();

    private final List<ZMQ.Socket> receivers = new ArrayList<>();

    private Listener<Pair<String, List<byte[]>>> serviceListener = null;

    private final ZContext context;

    public void registerSender(String taskId, String connectionAddress) {
        ZMQ.Socket requester = context.createSocket(SocketType.DEALER);
        requester.connect(connectionAddress);
        socketMap.put(taskId, requester);
    }

    public void registerReceiver(List<String> bindAddresses, Listener<Pair<String, List<byte[]>>> serviceListener) {
        for (String bindAddress : bindAddresses) {
            ZMQ.Socket receiveSocket = context.createSocket(SocketType.ROUTER);
            receiveSocket.bind(bindAddress);
            receivers.add(receiveSocket);
        }
        this.serviceListener = serviceListener;
    }

    public Communicator(ThreadHandler threadHandler, boolean isIndependentServer) {

        this.context = new ZContext();

        AsyncFunction asyncFunction = () -> {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    Timestamp comInitiateTimeStamp = new Timestamp(System.currentTimeMillis());

                    int requestTaskQueueSize = concurrentTaskQueue.size();

                    for (int i = 0; i < requestTaskQueueSize; i++) {

                        Pair<String, Pair<String, List<byte[]>>> serializedArrayWithIdentity = concurrentTaskQueue.poll();
                        // this can't be null due to the above implementation
                        String taskId = serializedArrayWithIdentity.getFirstElement();
                        Pair<String, List<byte[]>> requestPair = serializedArrayWithIdentity.getSecondElement();
                        String requestTokenId = requestPair.getFirstElement();
                        List<byte[]> serializedArrays = requestPair.getSecondElement();

                        sentTaskMap.putIfAbsent(taskId, new HashMap<>());
                        sentTaskMap.get(taskId).put(requestTokenId, serializedArrays);

                        ZMsg outMsg = new ZMsg();
                        outMsg.add(requestTokenId);
                        for (byte[] serializedArray : serializedArrays) {
                            outMsg.add(new ZFrame(serializedArray));
                        }
                        outMsg.send(socketMap.get(taskId));
                    }
                    socketMap.forEach((key, value) -> {
                        ZMsg inMsg = ZMsg.recvMsg(value, false);
                        if (inMsg != null) {
                            String responseTokenId = inMsg.pop().getString(StandardCharsets.UTF_8);
                            if (inMsg.size() > 0) {
                                byte[] response = inMsg.pop().getData();
                                replyListenerMap.remove(responseTokenId).listen(Optional.of(response));
                            } else {
                                replyListenerMap.remove(responseTokenId).listen(Optional.empty());
                            }
                            sentTaskMap.get(key).remove(responseTokenId);
                        }
                    });


                    receivers.forEach(receiver -> {
                        for (int receiveCount = 0; receiveCount < 10; receiveCount++) {
                            ZMsg inMsg = ZMsg.recvMsg(receiver, false);
                            if (inMsg != null) {
                                byte[] callerId = inMsg.pop().getData();
                                String clientTokenId = inMsg.pop().getString(StandardCharsets.UTF_8);
                                List<byte[]> requestArgs = new ArrayList<>();
                                while (inMsg.size() > 0) {
                                    requestArgs.add(inMsg.pop().getData());
                                }
                                serviceListener.listen(new Pair<>(clientTokenId, requestArgs));
                                pendingTaskMap.put(clientTokenId, callerId);
                            }
                        }

                        int completedTaskQueueSize = completedTaskQueue.size();

                        for (int i = 0; i < completedTaskQueueSize; i++) {

                            Pair<String, Optional<byte[]>> serializedArrayWithIdentity = completedTaskQueue.poll();
                            // this can't be null due to the above implementation
                            String clientTokenId = serializedArrayWithIdentity.getFirstElement();
                            ZMsg outMsg = new ZMsg();
                            outMsg.add(pendingTaskMap.get(clientTokenId));
                            outMsg.add(clientTokenId);
                            if (serializedArrayWithIdentity.getSecondElement().isPresent()) {
                                outMsg.add(serializedArrayWithIdentity.getSecondElement().get());
                            }
                            outMsg.send(receiver);
                            pendingTaskMap.remove(clientTokenId);
                        }
                        ;

                    });

                    Timestamp comCompletedTimeStamp = new Timestamp(System.currentTimeMillis());

                    long elapsedTime = comCompletedTimeStamp.getTime() - comInitiateTimeStamp.getTime();

                    if (elapsedTime < COMMUNICATOR_DELAY) {
                        Thread.sleep(COMMUNICATOR_DELAY - elapsedTime);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            }
        };

        threadHandler.initiateTaskRoutine(!isIndependentServer, asyncFunction);

    }

    public void sendRequest(String taskId, String tokenId, List<byte[]> serializedByteArrays) {
        concurrentTaskQueue.add(new Pair<>(taskId, new Pair<>(tokenId, serializedByteArrays)));
    }


    public void registerReplyListener(String tokenId, Listener<Optional<byte[]>> asyncFunction) {
        replyListenerMap.put(tokenId, asyncFunction);
    }

    public void sendReply(String tokenId, byte[] serializedByteArray) {
        completedTaskQueue.add(new Pair<>(tokenId, Optional.of(serializedByteArray)));
    }

    public void sendReply(String tokenId) {
        completedTaskQueue.add(new Pair<>(tokenId, Optional.empty()));
    }

}
