package github.ggb.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
public class PropertiesFileUtil {
    public PropertiesFileUtil() {
    }

    public static Properties readPropertiesFile(String fileName) {
        // TODO
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath = "";
        if (null != url) {
            rpcConfigPath = url.getPath() + fileName;
        }
        Properties properties = new Properties();
        try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)) {
            properties.load(inputStreamReader);
        } catch (IOException e) {
            log.error("occur exception when read properties file [{}]", fileName);
        }
        return properties;
    }

}