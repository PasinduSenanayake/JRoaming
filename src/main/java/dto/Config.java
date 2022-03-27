package dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private String serviceSignature;

    private int serviceParallelism;

    private boolean isStandAloneServer;

    private final List<String> bindAddresses = new ArrayList<>();

    private final Map<String,String> serviceMap = new HashMap<>();

    public boolean isStandAloneServer() {
        return isStandAloneServer;
    }

    public void setStandAloneServer(boolean standAloneServer) {
        isStandAloneServer = standAloneServer;
    }

    public void setServiceParallelism(int serviceParallelism) {
        this.serviceParallelism = serviceParallelism;
    }

    public void setServiceSignature(String serviceSignature) {
        this.serviceSignature = serviceSignature;
    }

    public void setBindAddresses(List<String> bindAddresses){
        this.bindAddresses.addAll(bindAddresses);
    }

    public void setServiceMap(Map<String,String> serviceMap){
        this.serviceMap.putAll(serviceMap);
    }

    public List<String> getBindAddresses() {
        return bindAddresses;
    }

    public String getServiceAddress(String serviceSignature) {
        return serviceMap.get(serviceSignature);
    }

    public String getServiceSignature() {
        return serviceSignature;
    }

    public int getServiceParallelism() {
        return serviceParallelism;
    }
}
