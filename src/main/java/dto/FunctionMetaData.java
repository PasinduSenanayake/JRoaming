package dto;

import marker.ForeignServiceImpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FunctionMetaData {

    private final String functionSignature;

    private final List<Method> paramParseList = new ArrayList<>();

    private final ForeignServiceImpl foreignService;

    private final Method function;

    private boolean isVoid;

    private boolean isReturnAsync = false;

    private Method returnParser;

    public FunctionMetaData(String functionSignature, ForeignServiceImpl foreignService,Method method){
        this.isVoid = true;
        this.functionSignature = functionSignature;
        this.foreignService = foreignService;
        this.function = method;
    }

    public Method getFunction() {
        return function;
    }

    public ForeignServiceImpl getForeignService() {
        return foreignService;
    }

    public String getFunctionSignature() {
        return functionSignature;
    }

    public List<Method> getParamParseList() {
        return paramParseList;
    }

    public void addInputParamParses(List<Method> paramParseList){
        this.paramParseList.addAll(paramParseList);
    }

    public void setReturnParser(Method returnParser, boolean isAsync){
        this.isVoid = false;
        this.returnParser = returnParser;
        this.isReturnAsync= isAsync;
    }

    public boolean isReturnAsync() {
        return isReturnAsync;
    }

    public Method getReturnParser() {
        return returnParser;
    }

    public boolean isVoid() {
        return isVoid;
    }
}
