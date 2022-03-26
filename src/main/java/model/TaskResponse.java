package model;

public class TaskResponse {

    private byte[] taskResponseMessage;


    public TaskResponse(byte[] taskResponseMessage) {
        this.taskResponseMessage = taskResponseMessage;

    }

    public TaskResponse() {
    }

    public byte[] getTaskResponseMessage() {
        return taskResponseMessage;
    }
}
