package github.ggb.extension;

public class Holder<T> {
    // 为什么要用volatile 这个地方没太懂 感觉不用getter setter直接用反而好点
    private volatile T value;
    public T get(){return value;}

    public void set(T value){this.value = value;}
}
