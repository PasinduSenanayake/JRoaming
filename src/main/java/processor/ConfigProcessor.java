package processor;

import dto.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigProcessor {

    public static Config generateConfig() {
        return parseConfigFile("service-discovery.yml");
    }

    public static Config generateConfig(String fileName) {
        return parseConfigFile(fileName);
    }

    private static void checkKeyExistence(String key, Map<String, ?> yamlData) {
        if (!yamlData.containsKey(key)) {
            throw new RuntimeException(key + " is required");
        }
    }

    private static Integer getServiceParallelism(Object parallelism) {
        Integer parallelCount;
        try {
            parallelCount = (Integer) parallelism;
        } catch (Exception e) {
            throw new RuntimeException("service-parallelism must be a positive integer");
        }
        if (parallelCount <= 0) {
            throw new RuntimeException("service-parallelism must be a positive integer");
        }
        return parallelCount;
    }

    private static String getNotEmptyString(String key, Object stringValue) {
        try {
            String convertValue = (String) stringValue;
            if (convertValue.equals("")) {
                throw new RuntimeException("value of " + key + " should be not be empty");
            }
            return convertValue;
        } catch (Exception e) {
            throw new RuntimeException("value of " + key + " should be String");
        }
    }

    private static String parseBindAddress(String serviceSignature, Map<String, String> addressMap) {
        checkKeyExistence("locality", addressMap);
        String connectionString;
        switch (addressMap.get("locality")) {
            case "remote":
                checkKeyExistence("connection-address", addressMap);
                connectionString = "tcp://" + getNotEmptyString("connection-address", addressMap.get("connection-address"));
                break;
            case "inter-process":
                connectionString = "ipc://" + serviceSignature;
                break;
            case "in-process":
                connectionString = "inproc://" + serviceSignature;
                break;
            default:
                throw new RuntimeException("locality must be remote, inter-process or in-process");
        }
        return connectionString;
    }

    private static void parseServerConfig(Config config, Map<String, Object> yamlData) {
        checkKeyExistence("service-signature", yamlData);
        checkKeyExistence("service-parallelism", yamlData);
        checkKeyExistence("bind-addresses", yamlData);
        checkKeyExistence("standalone-server", yamlData);
        config.setServiceSignature(getNotEmptyString("service-signature", yamlData.get("service-signature")));
        try {
            config.setStandAloneServer(Boolean.parseBoolean( yamlData.get("standalone-server").toString()));
        }catch (Exception e) {
            throw new RuntimeException("standalone-server should be either true or false");
        }

        config.setServiceParallelism(getServiceParallelism(yamlData.get("service-parallelism")));
        List<String> parsedAddresses = new ArrayList<>();
        try {
            List<Map<String, String>> bindAddresses = (List<Map<String, String>>) yamlData.get("bind-addresses");
            for (Map<String, String> address : bindAddresses) {
                parsedAddresses.add(parseBindAddress(config.getServiceSignature(), address));
            }
        } catch (Exception e) {
            throw new RuntimeException("bind-address is not parsable");
        }
        config.setBindAddresses(parsedAddresses);
    }

    private static void parseClientConfig(Config config, Map<String, Object> yamlData) {
        checkKeyExistence("services", yamlData);
        Map<String, String> serviceMap = new HashMap<>();
        try {
            List<Map<String, String>> serviceDataMaps = (List<Map<String, String>>) yamlData.get("services");
            for (Map<String, String> serviceDataMap : serviceDataMaps) {
                checkKeyExistence("service-signature", serviceDataMap);
                String serviceSignature = getNotEmptyString("service-signature", serviceDataMap.get("service-signature"));
                serviceMap.put(serviceSignature, parseBindAddress(serviceSignature, serviceDataMap));
            }
        } catch (Exception e) {
            throw new RuntimeException("bind-address is not parsable");
        }
        config.setServiceMap(serviceMap);
    }

    private static Config parseConfigFile(String parseConfigFile) {

        try {
            Config config = new Config();
            InputStream inputStream = ConfigProcessor.class.getClassLoader().getResourceAsStream(parseConfigFile);
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);

            checkKeyExistence("application", data);

            switch (data.get("application").toString()) {
                case "client":
                    parseClientConfig(config, data);
                    break;
                case "server":
                    parseServerConfig(config, data);
                    break;
                case "client-server":
                    parseServerConfig(config, data);
                    parseClientConfig(config, data);
                    break;
                default:
                    throw new RuntimeException("Application must be either client, server or client-server");
            }
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}


