package ru.nsu.vyaznikova.client;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public class KeyClient implements Callable<Integer> {

    @Option(names = {"-h", "--host"}, description = "Хост или DNS сервера", required = true)
    private String host;

    @Option(names = {"-p", "--port"}, description = "Порт сервера", required = true)
    private int port;

    @Option(names = {"-n", "--name"}, description = "Имя (ASCII), оканчивается NUL на протоколе", required = true)
    private String name;

    @Option(names = {"-d", "--delay"}, description = "Задержка в секундах перед чтением ответа (медленный клиент)")
    private int delaySeconds = 0;

    @Option(names = {"-a", "--abort"}, description = "Завершиться вместо чтения ответа (аварийное завершение)")
    private boolean abort;

    @Option(names = {"-o", "--out"}, description = "Каталог для сохранения файлов .key и .crt")
    private Path outDir = Path.of(".");

    @Override
    public Integer call() {
        System.out.println("[KeyClient] Параметры командной строки:");
        System.out.printf("  host = %s%n", host);
        System.out.printf("  port = %d%n", port);
        System.out.printf("  name = %s%n", name);
        System.out.printf("  delaySeconds = %d%n", delaySeconds);
        System.out.printf("  abort = %s%n", abort);
        System.out.printf("  outDir = %s%n", outDir.toAbsolutePath());
        return 0;
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new KeyClient()).execute(args);
        System.exit(exit);
    }
}
