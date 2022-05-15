package github.ggb.compress;

public interface Compress {
    byte[] compress(byte[] bytes);

    byte[] decomparess(byte[] bytes);
}
