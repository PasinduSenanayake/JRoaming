package processor;

import util.Listener;
import model.TaskRequest;
import model.TaskResponse;
import util.SecureRandomString;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class OffLoader {

    private final Communicator communicator;

    private final String foreignEntityName;

    private final String implSignature;

    public OffLoader(Communicator communicator, String foreignEntityName, String implSignature, String foreignEntityAddress) {
        this.communicator = communicator;
        this.foreignEntityName = foreignEntityName;
        this.implSignature = implSignature;
        this.communicator.registerSender(foreignEntityName, foreignEntityAddress);
    }

    public Future<TaskResponse> offloadTask(TaskRequest taskRequest) {

        String tokenId = implSignature + "::" + taskRequest.getMethodName() + "::" + SecureRandomString.generate();

        CompletableFuture<TaskResponse> taskDelegationResponse = new CompletableFuture<>();

        Listener<Optional<byte[]>> listener = (Optional<byte[]> receiveMessage) -> {
            try {
                if (receiveMessage.isPresent()) {
                    taskDelegationResponse.complete(new TaskResponse(receiveMessage.get()));
                } else {
                    taskDelegationResponse.complete(new TaskResponse());
                }

            } catch (Exception e) {
                taskDelegationResponse.completeExceptionally(e);

            }
        };


        communicator.registerReplyListener(tokenId, listener);

        this.communicator.sendRequest(foreignEntityName, tokenId, taskRequest.getSerializedMessages());


        return taskDelegationResponse;
    }

}
