package dto;

import model.TaskResponse;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncFuture implements Future {


    private final Future<?> completableFuture ;

    private final Method parseFrom;

    private final Class<?> aClass;


    public AsyncFuture(Future<?> completableFuture, Method parseFrom, Class<?> aClass){
        this.completableFuture =   completableFuture;
        this.parseFrom = parseFrom;
        this.aClass = aClass;
    }


    @Override
    public boolean cancel(boolean b) {
        return  this.completableFuture.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return this.completableFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.completableFuture.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        try{
            TaskResponse taskResponse = (TaskResponse)this.completableFuture.get();
            byte[] bytes = taskResponse.getTaskResponseMessage();
            Object obj = parseFrom.invoke(null,bytes);
            return aClass.cast(obj);
        }catch (InterruptedException | ExecutionException e){
            throw e;
        } catch (Exception e){
           throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
