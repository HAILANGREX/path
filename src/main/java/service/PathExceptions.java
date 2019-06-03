package service;

public class PathExceptions extends RuntimeException{

    //无参构造方法
    public PathExceptions(){
        super();
    }

    //有参的构造方法
    public PathExceptions(String message){
        super(message);
    }

    // 用指定的详细信息和原因构造一个新的异常
    public PathExceptions(String message, Throwable cause){
        super(message,cause);
    }

    //用指定原因构造一个新的异常
    public PathExceptions(Throwable cause) {
        super(cause);
    }
}
